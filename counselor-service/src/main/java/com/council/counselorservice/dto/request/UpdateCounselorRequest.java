package com.council.counselorservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateCounselorRequest {

    private String fullName;

    private Set<String> specializations;

    private String qualification;

    private int experienceYears;

    private String bio;

    private Double pricePerSession;

    private Boolean active;
}
