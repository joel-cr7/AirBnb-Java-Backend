package com.backend.project.Airbnb.strategy;

import com.backend.project.Airbnb.entity.Inventory;

import java.math.BigDecimal;


// Used to implement decorator design pattern, where we apply multiple pricing strategy to the price of room

public interface PricingStrategy {
    BigDecimal calculatePrice(Inventory inventory);
}
