package com.council.linkgereratorservice.controller;

import com.council.linkgereratorservice.dto.CreateMeetingLinkRequest;
import com.council.linkgereratorservice.dto.MeetingLinkResponse;
import com.council.linkgereratorservice.dto.UpdateMeetingLinkRequest;
import com.council.linkgereratorservice.service.MeetingLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/internal/meeting-links")
public class MeetingLinkController {

    private final MeetingLinkService meetingLinkService;

    public MeetingLinkController(MeetingLinkService meetingLinkService) {
        this.meetingLinkService = meetingLinkService;
    }

    @PostMapping
    public ResponseEntity<MeetingLinkResponse> createOrGet(@RequestBody CreateMeetingLinkRequest request) {
        return new ResponseEntity<>(meetingLinkService.createOrGet(request), HttpStatus.CREATED);
    }

    @GetMapping("/{appointmentId}")
    public MeetingLinkResponse getByAppointmentId(@PathVariable Long appointmentId) {
        return meetingLinkService.getByAppointmentId(appointmentId);
    }

    @GetMapping("/{appointmentId}/join")
    public ResponseEntity<Void> joinMeeting(@PathVariable Long appointmentId) {
        String link = meetingLinkService.getJoinLink(appointmentId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(link)).build();
    }

    @PutMapping("/{appointmentId}")
    public MeetingLinkResponse updateMeetingLink(
            @PathVariable Long appointmentId,
            @RequestBody UpdateMeetingLinkRequest request
    ) {
        return meetingLinkService.updateMeetingLink(appointmentId, request);
    }

    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> deleteMeetingLink(@PathVariable Long appointmentId) {
        meetingLinkService.deleteMeetingLink(appointmentId);
        return ResponseEntity.noContent().build();
    }
}
