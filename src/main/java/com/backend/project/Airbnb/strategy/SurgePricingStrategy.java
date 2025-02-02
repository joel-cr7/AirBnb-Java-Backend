package com.backend.project.Airbnb.strategy;

import com.backend.project.Airbnb.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;


// increase price based on surge factor in db

@RequiredArgsConstructor
public class SurgePricingStrategy implements PricingStrategy{

    // further modify or decorate the wrapped price based on SurgePricingStrategy
    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        return price.multiply(inventory.getSurgeFactor());
    }
}
