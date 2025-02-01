package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.BookingRequestDTO;
import com.backend.project.Airbnb.dto.GuestDTO;
import com.backend.project.Airbnb.entity.*;
import com.backend.project.Airbnb.entity.enums.BookingStatus;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.repository.*;
import com.backend.project.Airbnb.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final GuestRepository guestRepository;

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

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

        List<Inventory> inventories = inventoryRepository.findAndLockAvailableInventory(roomId,
                checkInDate,
                checkOutDate,
                bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(checkInDate, checkOutDate)+1;

        // we need exact no. of inventories as daysCount, if not it means we dont have enough inventory
        if(inventories.size() != daysCount){
            throw new IllegalStateException("Room is not available anymore !!");
        }

        // reserve the room/ update the booked count of inventories
        for(Inventory inventory: inventories){
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventories);


        // TODO: Calculate dynamic amount

        // Create Booking
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentuser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
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

        if(checkIfBookingExpired(booking)){
            throw new IllegalStateException("Booking has already expired !!");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state, cannot add guests !!");
        }

        for(GuestDTO guestDTO: guests){
            Guest guest = modelMapper.map(guestDTO, Guest.class);
            guest.setUser(getCurrentuser());
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDTO.class);

    }


    // A booking is expired if current time is greater than created time + 10 mins
    // Only keep booking active for 10 mins as booking is not confirmed
    private boolean checkIfBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }


    private User getCurrentuser(){
        //TODO: Remove dummy user
        User user = new User();
        user.setId(1L);
        return user;
    }
}
