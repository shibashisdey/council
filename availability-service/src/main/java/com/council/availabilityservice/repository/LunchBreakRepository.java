package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.LunchBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface LunchBreakRepository extends JpaRepository<LunchBreak, Long> {

    Optional<LunchBreak> findByCounselorIdAndDayOfWeek(Long counselorId, DayOfWeek dayOfWeek);

    Optional<LunchBreak> findTopByCounselorIdAndDayOfWeekIsNull(Long counselorId);

    List<LunchBreak> findByCounselorId(Long counselorId);

    void deleteByCounselorIdAndDayOfWeek(Long counselorId, DayOfWeek dayOfWeek);
}
