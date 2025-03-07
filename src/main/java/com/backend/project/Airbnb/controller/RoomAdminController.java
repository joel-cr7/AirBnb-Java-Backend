package com.backend.project.Airbnb.controller;

import com.backend.project.Airbnb.dto.RoomDTO;
import com.backend.project.Airbnb.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomAdminController {
    private final RoomService roomService;


    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAllRoomsInHotel(@PathVariable Long hotelId){
        List<RoomDTO> rooms = roomService.getAllRoomsInHotel(hotelId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long roomId){
        RoomDTO room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@PathVariable Long hotelId,
                                              @RequestBody RoomDTO roomDTO){
        log.info("Attempting to create a new room in hotel with ID: {}", hotelId);
        RoomDTO room = roomService.createRoomInHotel(hotelId, roomDTO);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomDTO> updateRoomById(@PathVariable Long hotelId,
                                                  @PathVariable Long roomId,
                                                  @RequestBody RoomDTO roomDTO){
        return ResponseEntity.ok(roomService.updateRoomById(hotelId, roomId, roomDTO));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long roomId){
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }
}
