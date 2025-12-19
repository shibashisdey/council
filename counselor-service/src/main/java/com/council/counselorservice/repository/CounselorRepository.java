package com.council.counselorservice.repository;

import com.council.counselorservice.model.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CounselorRepository extends JpaRepository<Counselor, Long> {
    Optional<Counselor> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<Counselor> findByActiveTrue();
}
