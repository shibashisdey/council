package com.council.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    private Long id; // SAME as auth-service userId

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String gender;

    private LocalDate dateOfBirth;

    @Column(length = 15)
    private String phoneNumber;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String city;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}