package com.backend.project.Airbnb.security;


import com.backend.project.Airbnb.dto.LoginDTO;
import com.backend.project.Airbnb.dto.SignupRequestDTO;
import com.backend.project.Airbnb.dto.UserDTO;
import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.entity.enums.Role;
import com.backend.project.Airbnb.exception.ResourceNotFoundException;
import com.backend.project.Airbnb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;


/**
 * Class responsible to handle user signup, login and provide refresh token
 */

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDTO signup(SignupRequestDTO signupRequestDTO){
        User user = userRepository.findByEmail(signupRequestDTO.getEmail()).orElse(null);

        if(user!=null){
            throw new RuntimeException("User is already present with same email id");
        }

        User newUser = modelMapper.map(signupRequestDTO, User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signupRequestDTO.getPassword()));

        return modelMapper.map(userRepository.save(newUser), UserDTO.class);
    }

    public String[] login(LoginDTO loginDTO){
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(), loginDTO.getPassword()
        ));

        User user = (User) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.getRefreshToken(user);
        return new String[]{accessToken, refreshToken};
    }

    public String refresh(String refreshToken){
        Long userId = jwtService.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: "+userId));

        return jwtService.generateAccessToken(user);
    }
}
