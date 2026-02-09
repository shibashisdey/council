package com.council.reviewservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "session_notes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"appointment_id"})
        },
        indexes = {
                @Index(name = "idx_session_notes_user_date", columnList = "user_id, session_date"),
                @Index(name = "idx_session_notes_counselor_date", columnList = "counselor_id, session_date")
        }
)
@Getter
@Setter
public class SessionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Column(nullable = false, length = 4000)
    private String observations;

    @Column(nullable = false, length = 4000)
    private String recommendations;

    @Column(name = "private_notes", length = 8000)
    private String privateNotes;

    @Column(name = "shared_with_client", nullable = false)
    private boolean sharedWithClient = false;

    @Column(name = "pdf_object_key")
    private String pdfObjectKey;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
