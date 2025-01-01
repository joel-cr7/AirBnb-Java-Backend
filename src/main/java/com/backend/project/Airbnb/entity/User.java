package com.backend.project.Airbnb.entity;


import com.backend.project.Airbnb.entity.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;


@Entity
@Getter
@Setter
@Table(name = "app_user")   // postgres does not allow table named "user"
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)  // this will create another table as "user_roles" to store all enum values mapping to user
    @Enumerated(EnumType.STRING)    // to persist enum values and applies check constraint, String means the values in DB must be enum values only and not numbers
    private Set<Role> roles;
}
