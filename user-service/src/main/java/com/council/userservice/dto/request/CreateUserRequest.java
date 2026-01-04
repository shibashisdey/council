package com.council.userservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateUserRequest {

    /**
     * User ID from user-authentication-service
     */
    //private Long userId;

    private String fullName;

    private LocalDate dateOfBirth;

    private String gender;

    private String phoneNumber;

    /**
     * Optional for now (can evolve later)
     */
    private String city;
}
