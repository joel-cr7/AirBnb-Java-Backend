package com.backend.project.Airbnb.dto;


import lombok.Data;

@Data
public class SignupRequestDTO {
    private String email;
    private String password;
    private String name;
}
