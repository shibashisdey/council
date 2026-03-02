package com.council.availabilityservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "pending_schedule_changes",
        indexes = {
                @Index(name = "idx_pending_schedule_lookup", columnList = "counselor_id, day_of_week, status, effective_from_date")
        }
)
@Getter
@Setter
public class PendingScheduleChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "lunch_start_time")
    private LocalTime lunchStartTime;

    @Column(name = "lunch_end_time")
    private LocalTime lunchEndTime;

    @Column(name = "off_day", nullable = false)
    private boolean offDay = false;

    @Column(name = "effective_from_date", nullable = false)
    private LocalDate effectiveFromDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingScheduleChangeStatus status = PendingScheduleChangeStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
