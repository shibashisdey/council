package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.CounselorWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorWorkingHoursRepository
        extends JpaRepository<CounselorWorkingHours, Long> {

    List<CounselorWorkingHours> findByCounselorId(Long counselorId);

    Optional<CounselorWorkingHours> findByCounselorIdAndDayOfWeek(
            Long counselorId,
            DayOfWeek dayOfWeek
    );

    void deleteByCounselorIdAndDayOfWeek(Long counselorId, DayOfWeek dayOfWeek);
}
