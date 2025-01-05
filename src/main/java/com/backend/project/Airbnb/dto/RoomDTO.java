package com.backend.project.Airbnb.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomDTO {
    // skipping hotel info here as fetch type is LAZY in Room entity
    private Long id;
    private String type;
    private BigDecimal basePrice;   // allow upto 10 digits and 2 decimal places
    private String[] photos;    // urls of photos
    private String[] amenities;     // room features
    private Integer totalCount;
    private Integer capacity;
}
