package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.entity.Booking;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.repository.BookingRepository;
import com.backend.project.Airbnb.service.CheckoutService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    private final BookingRepository bookingRepository;

    /**
     * Create a stripe Customer from the currently logged in user inorder to pass the customer details to stripe
     * Create Session params by providing the stripe customer and order related details (total price, booking related details)
     */
    @Override
    public String getCheckoutSession(Booking booking, String successURL, String failureURL) {
        log.info("Creating session for booking with id: {}",booking.getId());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            // create Stripe Customer from our user. This is done to pass user details to Stripe
            CustomerCreateParams createParams = CustomerCreateParams.builder()
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();
            Customer customer = Customer.create(createParams);

            // Create Stripe Session. Session params is needed to create stripe session
            SessionCreateParams sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)     // mandatory by Stripe for user to enter address during payment
                    .setSuccessUrl(successURL)      // if payment is successful, which url of our website should Stripe redirect
                    .setCancelUrl(failureURL)       // in-case failure happens, which url of our website should Stripe redirect
                    .setCustomer(customer.getId())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(      // here we pass all the metadata about the product that the customer is buying, for strip to show at checkout screen
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(booking.getAmount().multiply(BigDecimal.valueOf(100)).longValue())    // convert price in paise
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(booking.getHotel().getName() + " : " + booking.getRoom().getType())
                                                                    .setDescription("Booking ID: "+booking.getId())
                                                                    .build()
                                                    ).build()
                                    ).build()
                    ).build();

            Session session = Session.create(sessionParams);

            // we get session Id and session url from this session
            // store the session id with this particular booking id and return session url to client to proceed with payment
            booking.setPaymentSessionId(session.getId());
            bookingRepository.save(booking);

            log.info("Session created successfully for booking with id: {}",booking.getId());
            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}
