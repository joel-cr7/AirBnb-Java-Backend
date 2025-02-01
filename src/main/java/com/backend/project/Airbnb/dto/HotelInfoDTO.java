package com.backend.project.Airbnb.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HotelInfoDTO {
    private HotelDTO hotel;
    private List<RoomDTO> rooms;
}
