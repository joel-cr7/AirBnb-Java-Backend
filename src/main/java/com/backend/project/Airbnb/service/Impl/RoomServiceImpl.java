package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.RoomDTO;
import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Room;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.exception.UnauthorizedException;
import com.backend.project.Airbnb.repository.HotelRepository;
import com.backend.project.Airbnb.repository.RoomRepository;
import com.backend.project.Airbnb.service.InventoryService;
import com.backend.project.Airbnb.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.backend.project.Airbnb.util.AppUtils.getCurrentUser;


/**
 * Class responsible to handle room related operations in DB:
 * 1) Creating new Room type in a hotel
 * 2) Fetching all Room types in a hotel
 * 3) Fetching a Room type by its id
 * 4) Deleting a Room type by its id
 *
 * Note: As the /admin route is marked as authenticated in securityConfig, so we can get the currently
 * logged-in user.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;

    @Override
    public RoomDTO createRoomInHotel(Long hotelId, RoomDTO roomDTO) {
        log.info("Creating a new room in hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        // If current user is the owner of the hotel only then allow this operation
        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        Room room = modelMapper.map(roomDTO, Room.class);
        room.setHotel(hotel);
        room = roomRepository.save(room);

        // create inventory as soon as room is created and if hotel is active
        if(hotel.getActive()){
            inventoryService.initializeRoomForAYear(room);
        }

        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    public List<RoomDTO> getAllRoomsInHotel(Long hotelId) {
        log.info("Creating all rooms in hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        return hotel.getRooms()
                .stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .toList();
    }

    @Override
    public RoomDTO getRoomById(Long roomId) {
        log.info("Getting the rooms with ID: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        return modelMapper.map(room, RoomDTO.class);
    }

    @Override
    @Transactional
    public void deleteRoomById(Long roomId) {
        log.info("Deleting the rooms with ID: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(room.getHotel().getOwner())){
            throw new UnauthorizedException("Current user does not own room with id: " + roomId);
        }

        // delete all future inventory for this room.
        // first delete the inventories as room is referenced there
        inventoryService.deleteAllInventories(room);

        roomRepository.deleteById(roomId);
    }

    @Override
    public RoomDTO updateRoomById(Long hotelId, Long roomId, RoomDTO roomDTO) {
        log.info("Updating the room with ID: {}", roomId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        modelMapper.map(roomDTO, room);
        room.setId(roomId);

        room = roomRepository.save(room);

        return modelMapper.map(room, RoomDTO.class);
    }
}
