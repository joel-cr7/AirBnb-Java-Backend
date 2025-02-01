package com.backend.project.Airbnb.dto;

import lombok.Data;

import java.time.LocalDate;


@Data
public class HotelSearchRequestDTO {
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer roomsCount;

    // for paginated search result
    private Integer pageNumber = 0;
    private Integer pageSize = 10;
}
