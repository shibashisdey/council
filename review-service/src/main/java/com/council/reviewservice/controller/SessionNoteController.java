package com.council.reviewservice.controller;

import com.council.reviewservice.client.CounselorClient;
import com.council.reviewservice.dto.request.CreateSessionNoteRequest;
import com.council.reviewservice.dto.request.ShareSessionNoteRequest;
import com.council.reviewservice.dto.request.UpdatePdfRequest;
import com.council.reviewservice.dto.request.UpdateSessionNoteRequest;
import com.council.reviewservice.dto.response.SessionNoteClientResponse;
import com.council.reviewservice.dto.response.SessionNoteCounselorResponse;
import com.council.reviewservice.dto.response.SessionNotePublicResponse;
import com.council.reviewservice.service.SessionNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/session-notes")
public class SessionNoteController {

    private final SessionNoteService sessionNoteService;
    private final CounselorClient counselorClient;

    public SessionNoteController(SessionNoteService sessionNoteService, CounselorClient counselorClient) {
        this.sessionNoteService = sessionNoteService;
        this.counselorClient = counselorClient;
    }

    @PostMapping
    public ResponseEntity<SessionNoteCounselorResponse> createSessionNote(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @RequestBody CreateSessionNoteRequest request
    ) {
        requireTherapist(role);
        Long counselorId = requireCounselorId(requesterId);
        return ResponseEntity.ok(sessionNoteService.createSessionNote(counselorId, request));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<SessionNoteCounselorResponse> updateSessionNote(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long noteId,
            @RequestBody UpdateSessionNoteRequest request
    ) {
        requireTherapist(role);
        Long counselorId = requireCounselorId(requesterId);
        return ResponseEntity.ok(sessionNoteService.updateSessionNote(counselorId, noteId, request));
    }

    @PatchMapping("/{noteId}/share")
    public ResponseEntity<SessionNoteCounselorResponse> shareSessionNote(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long noteId,
            @RequestBody ShareSessionNoteRequest request
    ) {
        requireTherapist(role);
        Long counselorId = requireCounselorId(requesterId);
        return ResponseEntity.ok(sessionNoteService.shareSessionNote(counselorId, noteId, request));
    }

    @PatchMapping("/{noteId}/pdf")
    public ResponseEntity<Void> updatePdf(
            @PathVariable Long noteId,
            @RequestBody UpdatePdfRequest request
    ) {
        sessionNoteService.updatePdf(noteId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SessionNoteClientResponse>> getNotesForUser(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long userId
    ) {
        requireClient(role);
        if (!requesterId.equals(userId)) {
            throw new SecurityException("Not allowed to access these notes");
        }
        return ResponseEntity.ok(sessionNoteService.getNotesForUser(userId));
    }

    @GetMapping("/counselor/{counselorId}")
    public ResponseEntity<List<SessionNoteCounselorResponse>> getNotesForCounselor(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long counselorId
    ) {
        requireTherapist(role);
        Long requesterCounselorId = requireCounselorId(requesterId);
        if (!requesterCounselorId.equals(counselorId)) {
            throw new SecurityException("Not allowed to access these notes");
        }
        return ResponseEntity.ok(sessionNoteService.getNotesForCounselor(counselorId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<SessionNotePublicResponse> getNoteByAppointmentId(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(sessionNoteService.getNoteByAppointmentId(appointmentId));
    }

    /**
     * INTERNAL -> Used by notification service to fetch note by id
     */
    @GetMapping("/{noteId}/internal")
    public ResponseEntity<SessionNotePublicResponse> getNoteById(
            @PathVariable Long noteId
    ) {
        return ResponseEntity.ok(sessionNoteService.getNoteById(noteId));
    }

    private void requireTherapist(String role) {
        if (!"THERAPIST".equals(role)) {
            throw new SecurityException("Therapist access only");
        }
    }

    private void requireClient(String role) {
        if (!"CLIENT".equals(role)) {
            throw new SecurityException("Client access only");
        }
    }

    private Long requireCounselorId(Long requesterId) {
        var counselor = counselorClient.getCounselorByUserId(requesterId);
        if (counselor == null || counselor.getId() == null) {
            throw new SecurityException("Counselor profile not found");
        }
        return counselor.getId();
    }
}
