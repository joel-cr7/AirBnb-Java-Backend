package com.backend.project.Airbnb.repository;

import com.backend.project.Airbnb.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}