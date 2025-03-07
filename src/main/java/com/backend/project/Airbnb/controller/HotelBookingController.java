package com.backend.project.Airbnb.controller;


import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.BookingRequestDTO;
import com.backend.project.Airbnb.dto.GuestDTO;
import com.backend.project.Airbnb.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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


    /**
     * Initiate payment using Stripe payment gateway where stripe will generate session to make payment.
     * Stripe returns session URL where client can make payment
     * The process to generate session in Stripe is known as Checkout.
     * After the payment is completed by customer using the session URL, Stripe will send a webhook to an endpoint of
     * our application that we configure in Stripe CLI and the Stripe CLI will always be running to receive the webhook
     * and redirect that to our application endpoint which is in WebhookController
     * @param bookingId
     * @return session URL from Stripe
     */
    @PostMapping("/{bookingId}/payments")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long bookingId){
        String sessionURL = bookingService.initiatePayments(bookingId);
        return ResponseEntity.ok(Map.of("sessionUrl", sessionURL));
    }
    

    // cancel the booking and refund the payment
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId){
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }


    // Pooling API for frontend to know the payment status
    // frontend will keep calling this api to get the status
    @PostMapping("/{bookingId}/status")
    public ResponseEntity<Map<String, String>> getBookingStatus(@PathVariable Long bookingId){
        return ResponseEntity.ok(Map.of("status", bookingService.getBookingStatus(bookingId)));
    }

}
