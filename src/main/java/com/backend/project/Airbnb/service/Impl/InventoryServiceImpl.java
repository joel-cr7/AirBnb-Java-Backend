package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.RoomDTO;
import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Inventory;
import com.backend.project.Airbnb.entity.Room;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.repository.HotelRepository;
import com.backend.project.Airbnb.repository.InventoryRepository;
import com.backend.project.Airbnb.repository.RoomRepository;
import com.backend.project.Airbnb.service.InventoryService;
import com.backend.project.Airbnb.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    // add inventory for next 365 days ie. for a year in advance, this is done to keep track of rooms available/full
    // for a particular day
    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        while(!today.isAfter(endDate)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .date(today)
                    .price(room.getBasePrice())
                    .totalCount(room.getTotalCount())
                    .bookedCount(0)
                    .surgeFactor(BigDecimal.ONE)
                    .city(room.getHotel().getCity())
                    .closed(false)
                    .build();

            inventoryRepository.save(inventory);

            today = today.plusDays(1);
        }
    }


    @Override
    public void deleteFutureInventories(Room room) {
        LocalDate today = LocalDate.now();
        inventoryRepository.deleteByRoomAndHotelAndDateAfter(room, room.getHotel(), today);
    }
}
