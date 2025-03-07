package com.backend.project.Airbnb.controller;

import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.HotelDTO;
import com.backend.project.Airbnb.dto.HotelReportDTO;
import com.backend.project.Airbnb.service.BookingService;
import com.backend.project.Airbnb.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/admin/hotels")
@RequiredArgsConstructor
@Slf4j
public class HotelController {
    private final HotelService hotelService;
    private final BookingService bookingService;


    /**
     * API endpoint to get all hotels for this user (the current user will be admin as these are admin endpoints)
     */
    @GetMapping
    public ResponseEntity<List<HotelDTO>> getAllHotels(){
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    /**
     * API endpoint to get all bookings of a particular hotel
     */
    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingDTO>> getAllBookingsByHotelId(@PathVariable Long hotelId){
        return ResponseEntity.ok(bookingService.getBookingByHotel(hotelId));
    }

    /**
     * API endpoint for admin to get report of all confirmed bookings of all hotels owned by him.
     * Get report between custom input dates. If dates not specified default to 30 days
     */
    @GetMapping("/{hotelId}/report")
    public ResponseEntity<HotelReportDTO> getHotelReport(@PathVariable Long hotelId,
                                                               @RequestParam(required = false) LocalDate startDate,
                                                               @RequestParam(required = false) LocalDate endDate){
        if(startDate == null)   startDate = LocalDate.now().minusMonths(1);
        if(endDate == null)   endDate = LocalDate.now();
        return ResponseEntity.ok(bookingService.getHotelReport(hotelId, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<HotelDTO> createHotel(@RequestBody HotelDTO hotelDTO){
        log.info("Attempting to create a new hotel with name: {}", hotelDTO.getName());
        HotelDTO hotel = hotelService.createHotel(hotelDTO);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }

    @GetMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> getHotelById(@PathVariable Long hotelId){
        HotelDTO hotel = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<HotelDTO> updateHotelById(@PathVariable Long hotelId,
                                                    @RequestBody HotelDTO hotelDTO){
        HotelDTO hotel = hotelService.updateHotelById(hotelId, hotelDTO);
        return ResponseEntity.ok(hotel);
    }

    @PatchMapping("/{hotelId}/activate")
    public ResponseEntity<Void> activateHotel(@PathVariable Long hotelId){
        hotelService.activateHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId){
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }
}
