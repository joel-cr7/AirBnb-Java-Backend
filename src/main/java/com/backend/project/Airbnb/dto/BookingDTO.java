package com.backend.project.Airbnb.dto;

import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Room;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.entity.enums.BookingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


@Data
public class BookingDTO {
    private Long id;
    private Integer roomsCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BookingStatus bookingStatus;
    private Set<GuestDTO> guests;
}
