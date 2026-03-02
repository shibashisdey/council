package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.UnavailabilityReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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

    Optional<CounselorUnavailability> findTopByReferenceIdAndActiveTrueOrderByDateDesc(
            Long referenceId
    );

    boolean existsByReferenceIdAndActiveTrue(Long referenceId);

    long countByCounselorIdAndDateAndActiveTrueAndReason(
            Long counselorId,
            LocalDate date,
            UnavailabilityReason reason
    );

    List<CounselorUnavailability> findByReferenceId(Long referenceId);

    List<CounselorUnavailability> findByDateBefore(LocalDate cutoffDate);

    List<CounselorUnavailability> findByCounselorIdAndDateGreaterThanEqualAndActiveTrueAndReasonIn(
            Long counselorId,
            LocalDate date,
            List<UnavailabilityReason> reasons
    );

    List<CounselorUnavailability> findByCounselorIdAndDateGreaterThanEqualAndActiveTrueAndReasonOrderByDateAscStartTimeAsc(
            Long counselorId,
            LocalDate date,
            UnavailabilityReason reason
    );
}
