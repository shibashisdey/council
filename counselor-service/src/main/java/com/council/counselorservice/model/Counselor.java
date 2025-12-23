package com.council.counselorservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "counselors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id")
        }
)
@Getter
@Setter
public class Counselor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * userId from user-authentication-service
     * One therapist = one counselor profile
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Professional details
    @Column(nullable = false)
    private String fullName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "counselor_specializations",
            joinColumns = @JoinColumn(name = "counselor_id"),
            inverseJoinColumns = @JoinColumn(name = "specialization_id")
    )
    private Set<Specialization> specializations = new HashSet<>();


    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private Integer experienceYears;

    @Column(length = 1000)
    private String bio;

    // Business info
    @Column(nullable = false)
    private Double pricePerSession;

    @Column(nullable = false)
    private boolean active = true;

    // Audit fields
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
