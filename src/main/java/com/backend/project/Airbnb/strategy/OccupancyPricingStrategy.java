package com.backend.project.Airbnb.strategy;

import com.backend.project.Airbnb.entity.Inventory;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;


// increase price based on how many rooms are occupied

@RequiredArgsConstructor
public class OccupancyPricingStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);
        double occupancyRate = (double) inventory.getBookedCount() / inventory.getTotalCount();
        if(occupancyRate > 0.8){
            price = price.multiply(BigDecimal.valueOf(1.2));
        }
        return price;
    }
}
