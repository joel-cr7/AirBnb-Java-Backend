package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.entity.Booking;

public interface CheckoutService {
    String getCheckoutSession(Booking booking, String successURL, String failureURL);
}
