package com.backend.project.Airbnb.repository;

import com.backend.project.Airbnb.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
