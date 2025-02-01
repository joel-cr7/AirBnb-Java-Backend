package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.HotelDTO;
import com.backend.project.Airbnb.dto.HotelSearchRequestDTO;
import com.backend.project.Airbnb.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequest);
}
