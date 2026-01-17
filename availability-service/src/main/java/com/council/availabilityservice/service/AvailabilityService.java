package com.council.availabilityservice.service;
import com.council.availabilityservice.model.UnavailabilityReason;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
@Service
public interface AvailabilityService {

    /**
     * INTERNAL
     * Called by Appointment Service before booking
     */
    boolean isSlotAvailable(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );

    /**
     * INTERNAL
     * Lock slot for appointment (HOLD / CONFIRMED)
     */
    void blockSlot(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UnavailabilityReason reason,
            Long referenceId
    );

    /**
     * INTERNAL
     * Free slot when appointment cancelled / expired / rescheduled
     */
    void freeSlot(Long referenceId);
}
