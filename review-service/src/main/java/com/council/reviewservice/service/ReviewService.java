package com.council.reviewservice.service;

import com.council.reviewservice.dto.request.CreateReviewRequest;
import com.council.reviewservice.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(Long userId, CreateReviewRequest request);

    List<ReviewResponse> getReviewsForCounselor(Long counselorId);

    List<ReviewResponse> getReviewsForUser(Long userId);
}
