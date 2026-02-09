package com.council.availabilityservice.controller;

import com.council.availabilityservice.dto.request.BlockSlotRequest;
import com.council.availabilityservice.dto.request.UpdateBlockReasonRequest;
import com.council.availabilityservice.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/internal/availability")
public class InternalAvailabilityController {

    private final AvailabilityService availabilityService;

    public InternalAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> isSlotAvailable(
            @RequestParam Long counselorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        LocalTime effectiveEnd = endTime != null ? endTime : startTime.plusHours(1);
        boolean available = availabilityService.isSlotAvailable(counselorId, date, startTime, effectiveEnd);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/block")
    public ResponseEntity<Void> blockSlot(@RequestBody BlockSlotRequest request) {
        LocalTime endTime = request.getEndTime() != null
                ? request.getEndTime()
                : request.getStartTime().plusHours(1);
        availabilityService.blockSlot(
                request.getCounselorId(),
                request.getDate(),
                request.getStartTime(),
                endTime,
                request.getReason(),
                request.getReferenceId()
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/block/{referenceId}/reason")
    public ResponseEntity<Void> updateBlockReason(
            @PathVariable Long referenceId,
            @RequestBody UpdateBlockReasonRequest request
    ) {
        availabilityService.updateBlockReason(referenceId, request.getNewReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/free/{referenceId}")
    public ResponseEntity<Void> freeSlot(@PathVariable Long referenceId) {
        availabilityService.freeSlot(referenceId);
        return ResponseEntity.ok().build();
    }
}
