package com.backend.project.Airbnb.controller;


import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.BookingRequestDTO;
import com.backend.project.Airbnb.dto.GuestDTO;
import com.backend.project.Airbnb.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping("/init")
    public ResponseEntity<BookingDTO> initializeBooking(@RequestBody BookingRequestDTO bookingRequest){
        return ResponseEntity.ok(bookingService.initializeBooking(bookingRequest));
    }


    @PostMapping("/{bookingId}/addGuests")
    public ResponseEntity<BookingDTO> addGuests(@PathVariable Long bookingId,
                                                @RequestBody List<GuestDTO> guests){
        return ResponseEntity.ok(bookingService.addGuests(bookingId, guests));
    }

}
