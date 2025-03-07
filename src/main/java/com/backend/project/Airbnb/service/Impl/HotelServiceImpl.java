package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.HotelDTO;
import com.backend.project.Airbnb.dto.HotelInfoDTO;
import com.backend.project.Airbnb.dto.RoomDTO;
import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Room;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.exception.UnauthorizedException;
import com.backend.project.Airbnb.repository.HotelRepository;
import com.backend.project.Airbnb.repository.RoomRepository;
import com.backend.project.Airbnb.service.HotelService;
import com.backend.project.Airbnb.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.backend.project.Airbnb.util.AppUtils.getCurrentUser;


/**
 * Class responsible to handle admin operations related to hotels in DB
 * Note: As the /admin route is marked as authenticated in securityConfig, so we can get the currently
 * logged-in user.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final ModelMapper modelMapper;

    @Override
    public HotelDTO createHotel(HotelDTO hotelDTO) {
        log.info("Creating a new hotel with name: {}", hotelDTO.getName());
        Hotel hotel = modelMapper.map(hotelDTO, Hotel.class);

        User currentUser = getCurrentUser();
        hotel.setOwner(currentUser);

        hotel.setActive(false);
        hotel = hotelRepository.save(hotel);
        log.info("Created a new hotel with ID: {}", hotel.getId());
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    public HotelDTO getHotelById(Long hotelId) {
        log.info("Getting the hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        // If current user is the owner of the hotel only then allow this operation
        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    public HotelDTO updateHotelById(Long hotelId, HotelDTO hotelDTO) {
        log.info("Updating the hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        modelMapper.map(hotelDTO, hotel);
        hotel.setId(hotelId);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDTO.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long hotelId) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        // delete all the inventories and rooms for this hotel
        for(Room room: hotel.getRooms()){
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }

        hotelRepository.deleteById(hotelId);
    }

    @Override
    @Transactional
    public void activateHotel(Long hotelId) {
        log.info("Activating the hotel with ID: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        User currentUser = getCurrentUser();
        if(!currentUser.equals(hotel.getOwner())){
            throw new UnauthorizedException("Current user does not own hotel with id: " + hotelId);
        }

        hotel.setActive(true);

        // assuming we only do it once
        // create inventory for all the rooms of this hotel
        for(Room room: hotel.getRooms()){
            inventoryService.initializeRoomForAYear(room);
        }
    }

    @Override
    public HotelInfoDTO getHotelInfoById(Long hotelId) {
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));

        List<RoomDTO> rooms = hotel.getRooms()
                .stream()
                .map(room -> modelMapper.map(room, RoomDTO.class))
                .toList();

        return HotelInfoDTO.builder()
                .hotel(modelMapper.map(hotel, HotelDTO.class))
                .rooms(rooms)
                .build();
    }

    @Override
    public List<HotelDTO> getAllHotels() {
        User currentHotelOwner = getCurrentUser();
        log.info("Getting all hotels for the admin user with ID: {}", currentHotelOwner.getId());

        return hotelRepository.findByOwner(currentHotelOwner).stream()
                .map(hotel -> modelMapper.map(hotel, HotelDTO.class))
                .toList();
    }

}
