package com.council.availabilityservice.controller;

import com.council.availabilityservice.dto.request.BlockSlotRequest;
import com.council.availabilityservice.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/internal/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> isSlotAvailable(
            @RequestParam Long counselorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime
    ) {
        // Assuming 1-hour slots as per spec
        LocalTime endTime = startTime.plusHours(1);
        boolean available = availabilityService.isSlotAvailable(counselorId, date, startTime, endTime);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/block")
    public ResponseEntity<Void> blockSlot(@RequestBody BlockSlotRequest request) {
        availabilityService.blockSlot(
                request.getCounselorId(),
                request.getDate(),
                request.getStartTime(),
                request.getStartTime().plusHours(1), // 1-hour slot
                request.getReason(),
                request.getReferenceId()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/free/{referenceId}")
    public ResponseEntity<Void> freeSlot(@PathVariable Long referenceId) {
        availabilityService.freeSlot(referenceId);
        return ResponseEntity.ok().build();
    }
}
