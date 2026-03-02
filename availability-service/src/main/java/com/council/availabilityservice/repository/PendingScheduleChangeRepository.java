package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.PendingScheduleChange;
import com.council.availabilityservice.model.PendingScheduleChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingScheduleChangeRepository extends JpaRepository<PendingScheduleChange, Long> {

    Optional<PendingScheduleChange> findTopByCounselorIdAndDayOfWeekAndStatusOrderByCreatedAtDesc(
            Long counselorId,
            DayOfWeek dayOfWeek,
            PendingScheduleChangeStatus status
    );

    List<PendingScheduleChange> findByStatusAndEffectiveFromDateLessThanEqual(
            PendingScheduleChangeStatus status,
            LocalDate effectiveDate
    );
}

