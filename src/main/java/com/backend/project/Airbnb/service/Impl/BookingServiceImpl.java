package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.*;
import com.backend.project.Airbnb.entity.*;
import com.backend.project.Airbnb.entity.enums.BookingStatus;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.exception.UnauthorizedException;
import com.backend.project.Airbnb.repository.*;
import com.backend.project.Airbnb.service.BookingService;
import com.backend.project.Airbnb.service.CheckoutService;
import com.backend.project.Airbnb.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.backend.project.Airbnb.util.AppUtils.getCurrentUser;


/**
 * Class responsible to handle bookings of users and allows users to add guests
 * Note: As the /bookings route is marked as authenticated in securityConfig, so we can get the currently
 * logged-in user.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final CheckoutService checkoutService;
    private final PricingService pricingService;
    private final ModelMapper modelMapper;

    @Value("${frontend.url}")
    private String frontendURL;

    @Override
    @Transactional
    public BookingDTO initializeBooking(BookingRequestDTO bookingRequest) {
        Long hotelId = bookingRequest.getHotelId();
        Long roomId = bookingRequest.getRoomId();
        LocalDate checkInDate = bookingRequest.getCheckInDate();
        LocalDate checkOutDate = bookingRequest.getCheckOutDate();

        log.info("Initializing booking for hotel: {}, room: {}, date: {} - {}", hotelId, roomId, checkInDate,
                checkOutDate);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        List<Inventory> inventories = inventoryRepository.findAndLockAvailableInventory(
                roomId,
                checkInDate,
                checkOutDate,
                bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(checkInDate, checkOutDate)+1;

        // we need exact no. of inventories as daysCount, if not it means we dont have enough inventory
        if(inventories.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore !!");
        }

        // reserve the room/ update the booked count of inventories
        inventoryRepository.initBooking(
                room.getId(),
                checkInDate,
                checkOutDate,
                bookingRequest.getRoomsCount());


        // Calculate dynamic price
        BigDecimal dynamicPriceForOneRoom = pricingService.calculateTotalPrice(inventories);
        BigDecimal totalPriceForRequiredRooms = dynamicPriceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));


        // Create Booking
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPriceForRequiredRooms)
                .build();

        booking = bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDTO.class);

    }

    @Override
    @Transactional
    public BookingDTO addGuests(Long bookingId, List<GuestDTO> guests) {
        log.info("Adding guests for booking with ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // if current loggedIn user has not created this booking, throw exception
        User currentUser = getCurrentUser();

        if(!currentUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentUser.getId());
        }

        if(checkIfBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired !!");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state, cannot add guests !!");
        }

        for(GuestDTO guestDTO: guests){
            Guest guest = modelMapper.map(guestDTO, Guest.class);
            guest.setUser(currentUser);
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDTO.class);

    }

    @Override
    @Transactional
    public String initiatePayments(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: "+bookingId));

        User currentUser = getCurrentUser();

        if(!currentUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentUser.getId());
        }

        if(checkIfBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired !!");
        }

        String sessionURL = checkoutService.getCheckoutSession(
                booking,
                frontendURL + "payments/success",
                frontendURL + "payments/failure");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);

        return sessionURL;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        // get the data from the event (event contains all data about the payment, session, user, order, etc)
        // get the session id and check with the session id stored in DB (during session creation) if its same
        if("checkout.session.completed".equals(event.getType())){
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

            if(session == null) return;

            String sessionId = session.getId();
            Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found for Session with ID: "+ sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // acquire lock on DB records so we can update those records
            inventoryRepository.findAndLockReservedInventory(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount());

            // cant do both locking and updating in one query bcz one is modifying the records and annotated with @Modifying
            inventoryRepository.confirmBooking(
                    booking.getRoom().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getRoomsCount());

            log.info("Successfully confirmed the booking for booking ID: {}", booking.getId());

        }
        else{
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: "+ bookingId));

        User currentUser = getCurrentUser();

        if(!currentUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentUser.getId());
        }

        if(booking.getBookingStatus() != BookingStatus.CONFIRMED){
            throw new IllegalStateException("Only confirmed booking can be canceled !!");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // acquire lock on DB records so we can update those records
        inventoryRepository.findAndLockReservedInventory(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());

        // cant do bot locking and updating in one query bcz one is modifying the records and annotated with @Modifying
        inventoryRepository.cancelBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());

        // handle payment refund using session id stored in Booking
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundParams);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: "+ bookingId));

        User currentUser = getCurrentUser();

        if(!currentUser.equals(booking.getUser())){
            throw new UnauthorizedException("Booking does not belong to this user with id: "+currentUser.getId());
        }

        return booking.getBookingStatus().name();
    }

    @Override
    public List<BookingDTO> getBookingByHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentHotelOwner = getCurrentUser();

        log.info("Getting all bookings for hotel with ID: {}", hotelId);

        // check if current admin user is the owner of the hotel with id hotelId
        if(!currentHotelOwner.equals(hotel.getOwner())){
            throw new AccessDeniedException("You are not the owner of hotel with ID: "+hotelId);
        }

        return bookingRepository.findByHotel(hotel).stream()
                .map((element) -> modelMapper.map(element, BookingDTO.class))
                .toList();
    }

    @Override
    public HotelReportDTO getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentHotelOwner = getCurrentUser();

        log.info("Getting all bookings for hotel with ID: {}", hotelId);

        // check if current admin user is the owner of the hotel with id hotelId
        if(!currentHotelOwner.equals(hotel.getOwner())){
            throw new AccessDeniedException("You are not the owner of hotel with ID: "+hotelId);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(hotel, startDateTime, endDateTime);

        List<Booking> totalBookingsConfirmed = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .toList();

        Long totalBookingsCount = (long) totalBookingsConfirmed.size();
        BigDecimal totalRevenue = totalBookingsConfirmed.stream()
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenuePerBooking = totalBookingsCount!=0 ? totalRevenue.divide(BigDecimal.valueOf(totalBookingsCount), RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return HotelReportDTO.builder()
                .totalRevenue(totalRevenue)
                .bookingCount(totalBookingsCount)
                .avgRevenue(avgRevenuePerBooking)
                .build();
    }

    @Override
    public List<BookingDTO> getMyBookings() {
        User currentUser = getCurrentUser();
        return bookingRepository.findByUser(currentUser).stream()
                .map((booking) -> modelMapper.map(booking, BookingDTO.class))
                .collect(Collectors.toList());
    }


    // A booking is expired if current time is greater than created time + 10 mins
    // Only keep booking active for 10 mins as booking is not confirmed
    private boolean checkIfBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

}
