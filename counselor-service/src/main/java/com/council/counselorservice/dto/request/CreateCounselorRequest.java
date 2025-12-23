package com.council.counselorservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateCounselorRequest {

    //private Long userId; // from auth-service

    private String fullName;

    private Set<String> specializations; // names only

    private String qualification;

    private Integer experienceYears;

    private String bio;

    private Double pricePerSession;
}
