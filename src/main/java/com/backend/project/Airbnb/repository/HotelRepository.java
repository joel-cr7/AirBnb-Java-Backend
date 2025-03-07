package com.backend.project.Airbnb.repository;

import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    public List<Hotel> findByOwner(User owner);
}
