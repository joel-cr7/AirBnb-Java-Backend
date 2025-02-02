package com.backend.project.Airbnb.repository;

import com.backend.project.Airbnb.dto.HotelPriceDTO;
import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {
    // find hotels based on condition, and group by room type as same hotel might be having 2 different room type
    // for different days, but we want single room type that satisfy daysCount

    // use package name to specify, if JPQL must return custom class object
    // and invoke the constructor of that class
    @Query("""
            SELECT new com.backend.project.Airbnb.dto.HotelPriceDTO(i.hotel, AVG(i.price))
            FROM HotelMinPrice i
            WHERE i.hotel.city = :city
                AND i.hotel.active = true
                AND i.date BETWEEN :startDate AND :endDate
            GROUP BY i.hotel
            """)
    Page<HotelPriceDTO> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("daysCount") Long daysCount,  // no. of days between start and end date
            Pageable pageable
    );


    public Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);
}
