package com.backend.project.Airbnb.repository;

import com.backend.project.Airbnb.entity.Hotel;
import com.backend.project.Airbnb.entity.Inventory;
import com.backend.project.Airbnb.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    void deleteByRoomAndHotel(Room room, Hotel hotel);

    // find hotels based on condition, and group by room type as same hotel might be having 2 different room type
    // for different days, but we want single room type that satisfy daysCount
    @Query("""
            SELECT DISTINCT i.hotel
            FROM Inventory i
            WHERE i.city = :city
                AND i.closed = false
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
            GROUP BY i.hotel, i.room
            HAVING COUNT(i.date) = :daysCount
            """)
    Page<Hotel> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("daysCount") Long daysCount,  // no. of days between start and end date
            Pageable pageable
    );



    // lock the inventory records returned by query (so any other concurrent users trying to fetch same rows will
    // not be able to update)
    // the lock will be released after the transaction is complete (after the calling method ends)
    // lock records in db just for 1-2 ms
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.closed = false
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(@Param("roomId") Long roomId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("roomsCount") Integer roomsCount);


    // lock the inventory for the reserved rooms after the payment is done by customer so that we can update the booked
    // count of those records
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockReservedInventory(@Param("roomId") Long roomId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("numberOfRooms") Integer numberOfRooms);




    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.reservedCount = i.reservedCount + :numberOfRooms
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :numberOfRooms
                AND i.closed = false
            """)
    void initBooking(@Param("roomId") Long roomId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("numberOfRooms") int numberOfRooms);



    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.reservedCount = i.reservedCount - :numberOfRooms,
                i.bookedCount = i.bookedCount + :numberOfRooms
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                AND i.reservedCount >= :numberOfRooms
                AND i.closed = false
            """)
    void confirmBooking(@Param("roomId") Long roomId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("numberOfRooms") int numberOfRooms);



    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.bookedCount = i.bookedCount - :numberOfRooms
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
                AND (i.totalCount - i.bookedCount) >= :numberOfRooms
                AND i.closed = false
            """)
    void cancelBooking(@Param("roomId") Long roomId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("numberOfRooms") int numberOfRooms);


    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    List<Inventory> findByRoomOrderByDate(Room room);



    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findInventoryAndLockBeforeUpdate(@Param("roomId") Long roomId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);



    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.surgeFactor = :surgeFactor,
                i.closed = :closed
            WHERE i.room.id = :roomId
                AND i.date BETWEEN :startDate AND :endDate
            """)
    void updateInventory(@Param("roomId") Long roomId,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate,
                       @Param("surgeFactor") BigDecimal surgeFactor,
                       @Param("closed") boolean closed);
}
