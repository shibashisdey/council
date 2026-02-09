package com.council.appointmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBlockReasonRequest {
    private BlockSlotRequest.UnavailabilityReason newReason;
}
