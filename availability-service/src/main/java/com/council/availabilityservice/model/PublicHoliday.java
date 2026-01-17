package com.council.availabilityservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "public_holidays",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "holiday_date")
        }
)
@Getter
@Setter
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String countryCode; // IN, US, etc.
}
