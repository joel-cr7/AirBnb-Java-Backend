package com.backend.project.Airbnb.service.Impl;


import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.HotelMinPrice;
import com.backend.project.Airbnb.entity.Inventory;
import com.backend.project.Airbnb.repository.HotelMinPriceRepository;
import com.backend.project.Airbnb.repository.HotelRepository;
import com.backend.project.Airbnb.repository.InventoryRepository;
import com.backend.project.Airbnb.strategy.PricingService;
import com.backend.project.Airbnb.strategy.PricingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * Class responsible to handle the updation of prices of rooms from inventory based on dynamicPricing strategy
 * (using scheduler to update prices every hour)
 *
 * Also update the table that stores most minimum price room of the hotel on a particular day from the inventory
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingUpdateService {

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final PricingService pricingService;
    private final HotelMinPriceRepository hotelMinPriceRepository;


    // Scheduler to update the inventory and HotelMinPrice Table every hour
//    @Scheduled(cron = "*/5 * * * * *")
    @Scheduled(cron = "0 0 * * * *")
    public void updatePrices(){
        int page = 0;
        int batch = 100;

        while(true){
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batch));

            if(hotelPage.isEmpty()){
                break;
            }

            hotelPage.getContent().forEach(this::updateHotelPrices);

            page++;
        }
    }

    private void updateHotelPrices(Hotel hotel){
        log.info("Updating hotel prices for hotel ID: {}", hotel.getId());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusYears(1);
        List<Inventory> inventories = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        updateInventoryPrices(inventories);
        updateHotelMinPrices(hotel, inventories);
    }


    // Update the inventory prices based on dynamic pricing strategy
    private void updateInventoryPrices(List<Inventory> inventories){
        inventories.forEach(inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        });
        inventoryRepository.saveAll(inventories);
    }


    private void updateHotelMinPrices(Hotel hotel, List<Inventory> inventories) {
        // compute min price per day for the hotel
        Map<LocalDate, BigDecimal> dailyMinPrices = inventories.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(BigDecimal.ZERO)));

        // Prepare the HotelPrice entities in bulk
        List<HotelMinPrice> hotelPrices = new ArrayList<>();

        dailyMinPrices.forEach((date, price) -> {
            HotelMinPrice hotelMinPrice = hotelMinPriceRepository.findByHotelAndDate(hotel, date)
                    .orElse(new HotelMinPrice(hotel, date));
            hotelMinPrice.setPrice(price);
            hotelPrices.add(hotelMinPrice);
        });

        hotelMinPriceRepository.saveAll(hotelPrices);
    }

}
