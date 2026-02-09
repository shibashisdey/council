package com.council.reviewservice.repository;

import com.council.reviewservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByAppointmentId(Long appointmentId);

    List<Review> findByCounselorIdOrderByCreatedAtDesc(Long counselorId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
}
