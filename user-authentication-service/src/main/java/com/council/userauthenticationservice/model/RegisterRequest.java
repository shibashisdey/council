package com.council.userauthenticationservice.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    private String email;
    private String password;
    private Role role; // CLIENT or THERAPIST
}
