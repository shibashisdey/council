package com.council.appointmentservice.repository;

import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Check if a counselor slot is already taken
     * (CONFIRMED or PENDING_PAYMENT)
     */
    boolean existsByCounselorIdAndAppointmentDateAndStartTimeAndStatusIn(
            Long counselorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            List<AppointmentStatus> statuses
    );

    /**
     * Get all appointments of a counselor
     */
    List<Appointment> findByCounselorId(Long counselorId);

    /**
     * Get all appointments of a client
     */
    List<Appointment> findByClientId(Long clientId);

    /**
     * Find appointments whose payment hold expired
     * (used by scheduler)
     */
    List<Appointment> findByStatusAndSlotLockedAtBefore(
            AppointmentStatus status,
            LocalDateTime time
    );
    boolean existsByClientIdAndAppointmentDateAndStartTimeAndStatusIn(
            Long clientId,
            LocalDate date,
            LocalTime startTime,
            List<AppointmentStatus> statuses
    );
}
