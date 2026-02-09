package com.council.reviewservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"appointment_id"})
        },
        indexes = {
                @Index(name = "idx_reviews_counselor_created", columnList = "counselor_id, created_at"),
                @Index(name = "idx_reviews_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 4000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
