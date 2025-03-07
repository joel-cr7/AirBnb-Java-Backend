package com.backend.project.Airbnb.controller;


import com.backend.project.Airbnb.service.BookingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")      // this secret we get when we run stripe cli where we started the webhook listener
    private String webhookSecret;

    /**
     * Stripe webhook will call our this endpoint, which we configured in Stripe CLI.
     * Below is the method parameters of how Stripe will call our backend through webhook
     * Stripe will send payload and signatureHeader through which we ensure that only stripe is calling the endpoint
     */
    @PostMapping("/payment")
    public ResponseEntity<Void> capturePayments(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signHeader){
        try{
            Event event = Webhook.constructEvent(payload, signHeader, webhookSecret);   // verify that stripe is calling the endpoint
            bookingService.capturePayment(event);
            return ResponseEntity.noContent().build();
        }
        catch (SignatureVerificationException e) {
            throw new RuntimeException(e);
        }
    }

}
