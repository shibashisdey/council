package com.council.counselorservice.controller;

import com.council.counselorservice.dto.request.CreateCounselorRequest;
import com.council.counselorservice.dto.request.UpdateCounselorRequest;
import com.council.counselorservice.dto.response.CounselorResponse;
import com.council.counselorservice.service.CounselorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/counselors")
public class CounselorController {

    private final CounselorService counselorService;

    public CounselorController(CounselorService counselorService) {
        this.counselorService = counselorService;
    }

    /**
     * Create counselor profile
     * Role: THERAPIST
     */
    @PostMapping
    public ResponseEntity<CounselorResponse> createCounselor(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE") String role,
            @RequestBody CreateCounselorRequest request
    ) {
        CounselorResponse response =
                counselorService.createCounselor(userId, request, role);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get own counselor profile (THERAPIST)
     */
    @GetMapping("/me")
    public ResponseEntity<CounselorResponse> getMyProfile(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(counselorService.getByUserId(userId));
    }

    /**
     * Get counselor profile by userId
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CounselorResponse> getByUserId(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(counselorService.getByUserId(userId));
    }

    /**
     * Get all active counselors (for users to browse)
     */
    @GetMapping
    public ResponseEntity<Set<CounselorResponse>> getAllActiveCounselors() {
        return ResponseEntity.ok(counselorService.getAllActiveCounselors());
    }

    @GetMapping("/{id}")
    public CounselorResponse getCounselorById(@PathVariable Long id) {
        return counselorService.getById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CounselorResponse> updateCounselor(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE") String role,
            @RequestBody UpdateCounselorRequest request
    ) {
        CounselorResponse response =
                counselorService.updateCounselor(id, userId, role, request);
        return ResponseEntity.ok(response);
    }

}
