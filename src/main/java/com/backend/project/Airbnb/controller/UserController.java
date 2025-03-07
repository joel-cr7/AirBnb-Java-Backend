package com.backend.project.Airbnb.controller;


import com.backend.project.Airbnb.dto.BookingDTO;
import com.backend.project.Airbnb.dto.ProfileUpdateRequestDTO;
import com.backend.project.Airbnb.dto.UserDTO;
import com.backend.project.Airbnb.service.BookingService;
import com.backend.project.Airbnb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDTO profileUpdateRequestDTO){
        userService.updateProfile(profileUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/myBookings")
    public ResponseEntity<List<BookingDTO>> getMyBookings(){
        return ResponseEntity.ok(bookingService.getMyBookings());
    }


    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getMyProfile(){
        return ResponseEntity.ok(userService.getMyProfile());
    }

}
