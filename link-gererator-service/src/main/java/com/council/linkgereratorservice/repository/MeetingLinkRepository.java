package com.council.linkgereratorservice.repository;

import com.council.linkgereratorservice.model.MeetingLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingLinkRepository extends JpaRepository<MeetingLink, Long> {
    Optional<MeetingLink> findByAppointmentId(Long appointmentId);
    void deleteByAppointmentId(Long appointmentId);
}
