package com.council.availabilityservice.dto.request;

import com.council.availabilityservice.model.UnavailabilityReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBlockReasonRequest {
    private UnavailabilityReason newReason;
}
