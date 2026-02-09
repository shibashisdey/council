package com.council.availabilityservice.service;

import com.council.availabilityservice.model.UnavailabilityReason;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AvailabilityService {

    boolean isSlotAvailable(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );

    void blockSlot(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UnavailabilityReason reason,
            Long referenceId
    );

    void updateBlockReason(Long referenceId, UnavailabilityReason newReason);

    void freeSlot(Long referenceId);
}
