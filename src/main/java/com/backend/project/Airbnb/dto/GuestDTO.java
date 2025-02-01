package com.backend.project.Airbnb.dto;

import com.backend.project.Airbnb.entity.User;
import com.backend.project.Airbnb.entity.enums.Gender;
import lombok.Data;


@Data
public class GuestDTO {
    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;
}
