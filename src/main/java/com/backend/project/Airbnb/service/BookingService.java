package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.*;
import com.stripe.model.Event;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BookingService {
    BookingDTO initializeBooking(BookingRequestDTO bookingRequest);

    BookingDTO addGuests(Long bookingId, List<GuestDTO> guests);

    String initiatePayments(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    String getBookingStatus(Long bookingId);

    List<BookingDTO> getBookingByHotel(Long hotelId);

    HotelReportDTO getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate);

    List<BookingDTO> getMyBookings();
}
