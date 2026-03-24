package com.council.emailservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthUserResponse {
    private Long userId;
    private String email;
    private String role;
}
