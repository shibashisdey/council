package com.council.userauthenticationservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInternalResponse {
    private Long userId;
    private String email;
    private String role;
}
