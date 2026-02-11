package com.council.reviewservice.controller;

import com.council.reviewservice.client.CounselorClient;
import com.council.reviewservice.dto.request.CreateReviewRequest;
import com.council.reviewservice.dto.response.ReviewResponse;
import com.council.reviewservice.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CounselorClient counselorClient;

    public ReviewController(ReviewService reviewService, CounselorClient counselorClient) {
        this.reviewService = reviewService;
        this.counselorClient = counselorClient;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE") String role,
            @RequestBody CreateReviewRequest request
    ) {
        requireClient(role);
        return ResponseEntity.ok(reviewService.createReview(userId, request));
    }

    @GetMapping("/counselor/{counselorId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForCounselor(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long counselorId
    ) {
        requireTherapist(role);
        var counselor = counselorClient.getCounselorByUserId(requesterId);
        if (counselor == null || counselor.getId() == null || !counselor.getId().equals(counselorId)) {
            throw new SecurityException("Not allowed to access these reviews");
        }
        return ResponseEntity.ok(reviewService.getReviewsForCounselor(counselorId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForUser(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long userId
    ) {
        requireClient(role);
        if (!requesterId.equals(userId)) {
            throw new SecurityException("Not allowed to access these reviews");
        }
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId));
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
}
