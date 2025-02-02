package com.backend.project.Airbnb.strategy;

import com.backend.project.Airbnb.entity.Inventory;

import java.math.BigDecimal;


public class BasePricingStrategy implements PricingStrategy{
    // just return base price
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        return inventory.getRoom().getBasePrice();
    }
}
