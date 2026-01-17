package com.council.availabilityservice.controller;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.service.CounselorScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class CounselorScheduleController {

    private final CounselorScheduleService counselorScheduleService;

    public CounselorScheduleController(CounselorScheduleService counselorScheduleService) {
        this.counselorScheduleService = counselorScheduleService;
    }

    @PostMapping("/working-hours/{counselorId}")
    public ResponseEntity<Void> setWorkingHours(
            @PathVariable Long counselorId,
            @RequestBody SetWorkingHoursRequest request
    ) {
        counselorScheduleService.setWorkingHours(counselorId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unavailability/{counselorId}")
    public ResponseEntity<Void> addUnavailability(
            @PathVariable Long counselorId,
            @RequestBody AddUnavailabilityRequest request
    ) {
        counselorScheduleService.addUnavailability(counselorId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unavailability/{counselorId}/{unavailabilityId}")
    public ResponseEntity<Void> cancelUnavailability(
            @PathVariable Long counselorId,
            @PathVariable Long unavailabilityId
    ) {
        counselorScheduleService.cancelUnavailability(counselorId, unavailabilityId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/calendar/{counselorId}")
    public ResponseEntity<List<CounselorAvailabilityResponse>> getCalendarView(
            @PathVariable Long counselorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<CounselorAvailabilityResponse> availability =
                counselorScheduleService.getAvailabilityForDate(counselorId, date);
        return ResponseEntity.ok(availability);
    }
}
