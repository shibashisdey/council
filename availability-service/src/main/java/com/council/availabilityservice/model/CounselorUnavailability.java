package com.council.availabilityservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "counselor_unavailability",
        indexes = {
                @Index(
                        name = "idx_counselor_date",
                        columnList = "counselor_id, date"
                )
        }
)
@Getter
@Setter
public class CounselorUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnavailabilityReason reason;

    /**
     * Reference to appointmentId / leaveId etc.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Soft delete flag (important!)
     */
    @Column(nullable = false)
    private boolean active = true;
}
