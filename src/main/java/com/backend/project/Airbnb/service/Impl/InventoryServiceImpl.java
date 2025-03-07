package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.*;
import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Inventory;
import com.backend.project.Airbnb.entity.Room;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.repository.HotelMinPriceRepository;
import com.backend.project.Airbnb.repository.InventoryRepository;
import com.backend.project.Airbnb.repository.RoomRepository;
import com.backend.project.Airbnb.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.backend.project.Airbnb.util.AppUtils.getCurrentUser;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final ModelMapper modelMapper;

    // add inventory for next 365 days ie. for a year in advance, this is done to keep track of rooms available/full
    // for a particular day
    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        while(!today.isAfter(endDate)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .date(today)
                    .price(room.getBasePrice())
                    .totalCount(room.getTotalCount())
                    .bookedCount(0)
                    .reservedCount(0)
                    .surgeFactor(BigDecimal.ONE)
                    .city(room.getHotel().getCity())
                    .closed(false)
                    .build();

            inventoryRepository.save(inventory);

            today = today.plusDays(1);
        }
    }


    @Override
    public void deleteAllInventories(Room room) {
        log.info("Deleting the inventories of room with ID: {}", room.getId());
        inventoryRepository.deleteByRoomAndHotel(room, room.getHotel());
    }

    @Override
    public Page<HotelPriceDTO> searchHotels(HotelSearchRequestDTO hotelSearchRequest) {
        log.info("Searching hotels for {} city, from {} to {}",hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());

        Pageable pageable = PageRequest.of(hotelSearchRequest.getPageNumber(), hotelSearchRequest.getPageSize());

        // adding 1 to handle same day start and end date
        long daysCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;

        Page<HotelPriceDTO> hotelPage = hotelMinPriceRepository.findHotelsWithAvailableInventory(
                hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),
                daysCount,
                pageable
        );

        return hotelPage;
    }

    @Override
    public List<InventoryDTO> getAllInventoryByRoom(Long roomId) {
        log.info("Getting all inventory by room for room with ID: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        User currentAdminUser = getCurrentUser();
        if(!currentAdminUser.equals(room.getHotel().getOwner())){
            throw new AccessDeniedException("You are not owner of room with ID: " + roomId);
        }

        return inventoryRepository.findByRoomOrderByDate(room).stream()
                .map((element) -> modelMapper.map(element, InventoryDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryRequestDTO updateInventoryRequestDTO) {
        log.info("Updating all inventory by room for room with ID: {} between date range {} to {}",
                roomId, updateInventoryRequestDTO.getStartDate(), updateInventoryRequestDTO.getEndDate());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        User currentAdminUser = getCurrentUser();
        if(!currentAdminUser.equals(room.getHotel().getOwner())){
            throw new AccessDeniedException("You are not owner of room with ID: " + roomId);
        }

        inventoryRepository.findInventoryAndLockBeforeUpdate(roomId, updateInventoryRequestDTO.getStartDate(),
                updateInventoryRequestDTO.getEndDate());

        inventoryRepository.updateInventory(roomId, updateInventoryRequestDTO.getStartDate(),
                updateInventoryRequestDTO.getEndDate(), updateInventoryRequestDTO.getSurgeFactor(),
                updateInventoryRequestDTO.getClosed());

    }
}
