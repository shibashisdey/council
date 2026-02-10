package com.council.notificationservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CounselorResponse {
    private Long id;
    private Long userId;
    private String fullName;
}
