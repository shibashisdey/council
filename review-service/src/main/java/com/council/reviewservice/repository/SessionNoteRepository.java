package com.council.reviewservice.repository;

import com.council.reviewservice.model.SessionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    Optional<SessionNote> findByAppointmentId(Long appointmentId);

    List<SessionNote> findByUserIdAndSharedWithClientTrueOrderBySessionDateDesc(Long userId);

    List<SessionNote> findByCounselorIdOrderBySessionDateDesc(Long counselorId);
}
