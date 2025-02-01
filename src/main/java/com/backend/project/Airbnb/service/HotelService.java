package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.HotelDTO;
import com.backend.project.Airbnb.dto.HotelInfoDTO;
import com.backend.project.Airbnb.entity.Hotel;

public interface HotelService {
    HotelDTO createHotel(HotelDTO hotelDTO);

    HotelDTO getHotelById(Long hotelId);

    HotelDTO updateHotelById(Long hotelId, HotelDTO hotelDTO);

    void deleteHotelById(Long hotelId);

    void activateHotel(Long hotelId);

    HotelInfoDTO getHotelInfoById(Long hotelId);
}
