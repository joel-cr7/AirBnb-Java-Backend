package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.RoomDTO;

import java.util.List;

public interface RoomService {
    RoomDTO createRoomInHotel(Long hotelId, RoomDTO roomDTO);

    List<RoomDTO> getAllRoomsInHotel(Long hotelId);

    RoomDTO getRoomById(Long roomId);

    void deleteRoomById(Long roomId);
}
