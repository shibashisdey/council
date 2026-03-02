package com.council.availabilityservice.repository;

import com.council.availabilityservice.model.LunchBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LunchBreakRepository extends JpaRepository<LunchBreak, Long> {

    Optional<LunchBreak> findByCounselorId(Long counselorId);

    void deleteByCounselorId(Long counselorId);
}
