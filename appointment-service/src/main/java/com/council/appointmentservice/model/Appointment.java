package com.council.appointmentservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "appointments",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "counselor_id",
                                "appointment_date",
                                "start_time"
                        }
                )
        }
)
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID of CLIENT (from user-auth service)
     */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /**
     * User ID of THERAPIST (from counselor-service)
     */
    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    /**
     * Appointment date (no time)
     */
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    /**
     * Start time (1-hour slot)
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time (derived, but stored for simplicity)
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Booking status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    /**
     * Payment reference (filled after payment service integration)
     */
    @Column(name = "payment_id")
    private String paymentId;

    /**
     * When slot was locked for payment
     */
    @Column(name = "slot_locked_at")
    private LocalDateTime slotLockedAt;

    /**
     * Audit fields
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
