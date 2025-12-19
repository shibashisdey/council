package com.council.counselorservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
@Builder
@Getter
@Setter
public class CounselorResponse {

    private Long id;

    private Long userId;

    private String fullName;

    private Set<String> specializations;

    private String qualification;

    private int experienceYears;

    private String bio;

    private Double pricePerSession;

    private boolean active;
}
