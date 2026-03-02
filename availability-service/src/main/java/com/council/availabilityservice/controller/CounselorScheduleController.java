package com.council.availabilityservice.controller;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SafeWorkingHoursUpdateRequest;
import com.council.availabilityservice.dto.request.SetLunchBreakRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.dto.response.CounselorScheduleResponse;
import com.council.availabilityservice.dto.response.SafeWorkingHoursUpdateResponse;
import com.council.availabilityservice.dto.response.UpcomingLeaveResponse;
import com.council.availabilityservice.service.CounselorScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
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

    @PostMapping("/lunch-break/{counselorId}")
    public ResponseEntity<Void> setLunchBreak(
            @PathVariable Long counselorId,
            @RequestBody SetLunchBreakRequest request
    ) {
        counselorScheduleService.setLunchBreak(counselorId, request);
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

    @GetMapping("/unavailability/{counselorId}/upcoming")
    public ResponseEntity<List<UpcomingLeaveResponse>> getUpcomingLeaves(@PathVariable Long counselorId) {
        return ResponseEntity.ok(counselorScheduleService.getUpcomingLeaves(counselorId));
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

    @GetMapping("/{counselorId}")
    public ResponseEntity<CounselorScheduleResponse> getSchedule(@PathVariable Long counselorId) {
        return ResponseEntity.ok(counselorScheduleService.getSchedule(counselorId));
    }

    @PutMapping("/working-hours-safe/{counselorId}")
    public ResponseEntity<SafeWorkingHoursUpdateResponse> updateWorkingHoursSafely(
            @PathVariable Long counselorId,
            @RequestBody SafeWorkingHoursUpdateRequest request
    ) {
        return ResponseEntity.ok(counselorScheduleService.updateWorkingHoursSafely(counselorId, request));
    }

    @DeleteMapping("/working-hours-safe/{counselorId}/{dayOfWeek}")
    public ResponseEntity<SafeWorkingHoursUpdateResponse> removeWorkingDaySafely(
            @PathVariable Long counselorId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        return ResponseEntity.ok(counselorScheduleService.removeWorkingDaySafely(counselorId, dayOfWeek));
    }
}
