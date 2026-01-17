package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.CounselorUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CounselorUnavailabilityRepository
        extends JpaRepository<CounselorUnavailability, Long> {

    List<CounselorUnavailability> findByCounselorIdAndDateAndActiveTrue(
            Long counselorId,
            LocalDate date
    );

    boolean existsByCounselorIdAndDateAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
            Long counselorId,
            LocalDate date,
            LocalTime endTime,
            LocalTime startTime
    );

    List<CounselorUnavailability> findByCounselorIdAndActiveTrue(
            Long counselorId
    );

    List<CounselorUnavailability> findByDateBefore(LocalDate cutoffDate);
}
