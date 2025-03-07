package com.backend.project.Airbnb.service.Impl;

import com.backend.project.Airbnb.dto.ProfileUpdateRequestDTO;
import com.backend.project.Airbnb.dto.UserDTO;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.repository.UserRepository;
import com.backend.project.Airbnb.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.backend.project.Airbnb.util.AppUtils.getCurrentUser;


/**
 * Class responsible to handle user related operations from DB
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found with ID: "+userId));
    }

    @Override
    public void updateProfile(ProfileUpdateRequestDTO profileUpdateRequestDTO) {
        User currentUser = getCurrentUser();

        if(profileUpdateRequestDTO.getDateOfBirth() != null){
            currentUser.setDateOfBirth(profileUpdateRequestDTO.getDateOfBirth());
        }
        if(profileUpdateRequestDTO.getName() != null){
            currentUser.setName(profileUpdateRequestDTO.getName());
        }
        if(profileUpdateRequestDTO.getGender() != null){
            currentUser.setGender(profileUpdateRequestDTO.getGender());
        }

        userRepository.save(currentUser);
    }

    @Override
    public UserDTO getMyProfile() {
        User user = getCurrentUser();
        log.info("Getting the profile for user with ID: {}", user.getId());
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email "+email));
    }
}
