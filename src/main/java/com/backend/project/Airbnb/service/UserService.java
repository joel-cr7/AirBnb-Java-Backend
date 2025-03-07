package com.backend.project.Airbnb.service;

import com.backend.project.Airbnb.dto.ProfileUpdateRequestDTO;
import com.backend.project.Airbnb.dto.UserDTO;
import com.backend.project.Airbnb.entity.User;

public interface UserService {
    public User getUserById(Long userId);

    void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO);

    UserDTO getMyProfile();
}
