package com.council.emailservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileResponse {
    private Long userId;
    private String fullName;
}
