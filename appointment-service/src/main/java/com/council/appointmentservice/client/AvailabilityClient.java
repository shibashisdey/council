package com.council.appointmentservice.client;

import com.council.appointmentservice.dto.BlockSlotRequest;

import java.time.LocalDate;
import java.time.LocalTime;

public interface AvailabilityClient {

    boolean isSlotAvailable(Long counselorId, LocalDate date, LocalTime startTime);

    void blockSlot(BlockSlotRequest request);

    void freeSlot(Long referenceId);
}
