package com.council.appointmentservice.service;

import com.council.appointmentservice.dto.request.CreateAppointmentRequest;
import com.council.appointmentservice.dto.request.RescheduleAppointmentRequest;
import com.council.appointmentservice.dto.response.AppointmentResponse;
import com.council.appointmentservice.dto.response.CounselorAppointmentResponse;
import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.model.AppointmentStatus;
import com.council.appointmentservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final int SLOT_DURATION_HOURS = 1;
    private static final int PAYMENT_HOLD_MINUTES = 10;

    private final AppointmentRepository appointmentRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * CLIENT books appointment → slot locked
     */
    @Override
    public AppointmentResponse createAppointment(
            Long clientId,
            CreateAppointmentRequest request
    ) {

        // 1️⃣ Check slot availability
        boolean slotTaken =
                appointmentRepository.existsByCounselorIdAndAppointmentDateAndStartTimeAndStatusIn(
                        request.getCounselorId(),
                        request.getAppointmentDate(),
                        request.getStartTime(),
                        List.of(
                                AppointmentStatus.CONFIRMED,
                                AppointmentStatus.PENDING_PAYMENT
                        )
                );

        if (slotTaken) {
            throw new IllegalStateException("Slot already booked");
        }

        // 2️⃣ Create appointment
        Appointment appointment = new Appointment();
        appointment.setClientId(clientId);
        appointment.setCounselorId(request.getCounselorId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getStartTime().plusHours(SLOT_DURATION_HOURS));
        appointment.setStatus(AppointmentStatus.PENDING_PAYMENT);
        appointment.setSlotLockedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        return mapToResponse(saved);
    }

    /**
     * CLIENT views his appointments
     */
    @Override
    public List<AppointmentResponse> getAppointmentsForClient(Long clientId) {
        return appointmentRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * COUNSELOR views his appointments
     */
    @Override
    public List<CounselorAppointmentResponse> getAppointmentsForCounselor(Long counselorId) {
        return appointmentRepository.findByCounselorId(counselorId)
                .stream()
                .map(this::mapToCounselorResponse)
                .toList();
    }

    /**
     * CLIENT reschedules appointment
     */
    @Override
    public AppointmentResponse rescheduleAppointment(
            Long appointmentId,
            Long clientId,
            RescheduleAppointmentRequest request
    ) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (!appointment.getClientId().equals(clientId)) {
            throw new SecurityException("Not allowed to reschedule this appointment");
        }

        // TODO: enforce 12-hour rule later

        // Check new slot availability
        boolean slotTaken =
                appointmentRepository.existsByCounselorIdAndAppointmentDateAndStartTimeAndStatusIn(
                        appointment.getCounselorId(),
                        request.getNewDate(),
                        request.getNewStartTime(),
                        List.of(
                                AppointmentStatus.CONFIRMED,
                                AppointmentStatus.PENDING_PAYMENT
                        )
                );

        if (slotTaken) {
            throw new IllegalStateException("New slot is not available");
        }

        appointment.setAppointmentDate(request.getNewDate());
        appointment.setStartTime(request.getNewStartTime());
        appointment.setEndTime(request.getNewStartTime().plusHours(SLOT_DURATION_HOURS));
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setSlotLockedAt(LocalDateTime.now());

        Appointment updated = appointmentRepository.save(appointment);
        return mapToResponse(updated);
    }

    /**
     * CANCEL appointment
     */
    @Override
    public void cancelAppointment(
            Long appointmentId,
            Long requesterId,
            String requesterRole
    ) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        boolean isClient =
                "CLIENT".equals(requesterRole)
                        && appointment.getClientId().equals(requesterId);

        boolean isCounselor =
                "THERAPIST".equals(requesterRole)
                        && appointment.getCounselorId().equals(requesterId);

        if (!isClient && !isCounselor) {
            throw new SecurityException("Not allowed to cancel this appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    // =======================
    // MAPPERS
    // =======================

    private AppointmentResponse mapToResponse(Appointment a) {
        return AppointmentResponse.builder()
                .appointmentId(a.getId())
                .clientId(a.getClientId())
                .counselorId(a.getCounselorId())
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .paymentId(a.getPaymentId())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private CounselorAppointmentResponse mapToCounselorResponse(Appointment a) {
        return CounselorAppointmentResponse.builder()
                .appointmentId(a.getId())
                .clientId(a.getClientId())
                .appointmentDate(a.getAppointmentDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .build();
    }
}
