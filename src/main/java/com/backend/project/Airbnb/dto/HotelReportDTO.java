package com.backend.project.Airbnb.dto;


import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelReportDTO {
    private Long bookingCount;
    private BigDecimal totalRevenue;
    private BigDecimal avgRevenue;      // avg revenue per booking
}
