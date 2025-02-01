package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.BookingRequestDTO;
import com.backend.project.Airbnb.dto.GuestDTO;

import java.util.List;

public interface BookingService {
    BookingDTO initializeBooking(BookingRequestDTO bookingRequest);

    BookingDTO addGuests(Long bookingId, List<GuestDTO> guests);
}
