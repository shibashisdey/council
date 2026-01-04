package com.council.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long userId;

    private String fullName;

    private Integer age;

    private String gender;

    private String phoneNumber;

    private String city;
}
