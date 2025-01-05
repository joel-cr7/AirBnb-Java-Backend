package com.backend.project.Airbnb.dto;


import com.backend.project.Airbnb.entity.HotelContactInfo;
import lombok.Data;

@Data
public class HotelDTO {
    private Long id;
    private String name;
    private String city;
    private String[] photos;    // urls of photos
    private String[] amenities;     // features like ['WiFi', 'Swimming pool']
    private Boolean active;
    private HotelContactInfo contactInfo;
}
