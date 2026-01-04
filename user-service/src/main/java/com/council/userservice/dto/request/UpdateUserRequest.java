package com.council.userservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserRequest {


    private String fullName;

    private String email;

    private String phoneNumber;

    private String gender;

    private LocalDate dateOfBirth;

    private String city;
}
