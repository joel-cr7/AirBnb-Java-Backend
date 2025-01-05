package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.HotelDTO;
import com.backend.project.Airbnb.entity.Room;

public interface InventoryService {
    void initializeRoomForAYear(Room room);

    void deleteFutureInventories(Room room);
}
