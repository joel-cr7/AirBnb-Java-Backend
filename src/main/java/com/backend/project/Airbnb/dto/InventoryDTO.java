package com.backend.project.Airbnb.dto;


import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryDTO {
    private Long id;

    private LocalDate date;

    private Integer bookedCount;

    private Integer reservedCount;

    private Integer totalCount;

    private BigDecimal surgeFactor;

    private BigDecimal price;

    private Boolean closed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
