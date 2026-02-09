package com.council.reviewservice.service;

import com.council.reviewservice.client.AppointmentClient;
import com.council.reviewservice.dto.request.CreateReviewRequest;
import com.council.reviewservice.dto.response.AppointmentInternalResponse;
import com.council.reviewservice.dto.response.ReviewResponse;
import com.council.reviewservice.model.Review;
import com.council.reviewservice.repository.ReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentClient appointmentClient;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            AppointmentClient appointmentClient
    ) {
        this.reviewRepository = reviewRepository;
        this.appointmentClient = appointmentClient;
    }

    @Override
    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        AppointmentInternalResponse appointment = getAppointmentOrThrow(request.getAppointmentId());

        if (!userId.equals(appointment.getClientId())) {
            throw new SecurityException("Not allowed to review this appointment");
        }
        if (!"COMPLETED".equals(appointment.getStatus())) {
            throw new IllegalStateException("Appointment is not completed");
        }

        Review review = new Review();
        review.setAppointmentId(appointment.getAppointmentId());
        review.setUserId(appointment.getClientId());
        review.setCounselorId(appointment.getCounselorId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        try {
            Review saved = reviewRepository.save(review);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Review already exists for this appointment", e);
        }
    }

    @Override
    public List<ReviewResponse> getReviewsForCounselor(Long counselorId) {
        return reviewRepository.findByCounselorIdOrderByCreatedAtDesc(counselorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsForUser(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AppointmentInternalResponse getAppointmentOrThrow(Long appointmentId) {
        try {
            AppointmentInternalResponse response = appointmentClient.getAppointmentInternal(appointmentId);
            if (response == null) {
                throw new IllegalStateException("Appointment not found");
            }
            return response;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Appointment service unavailable", e);
        }
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .appointmentId(review.getAppointmentId())
                .userId(review.getUserId())
                .counselorId(review.getCounselorId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
