package com.council.appointmentservice.service;

import com.council.appointmentservice.client.AvailabilityClient;
import com.council.appointmentservice.dto.BlockSlotRequest;
import com.council.appointmentservice.dto.request.CreateAppointmentRequest;
import com.council.appointmentservice.dto.request.RescheduleAppointmentRequest;
import com.council.appointmentservice.dto.response.AppointmentResponse;
import com.council.appointmentservice.dto.response.CounselorAppointmentResponse;
import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.model.AppointmentStatus;
import com.council.appointmentservice.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.council.appointmentservice.dto.BlockSlotRequest.UnavailabilityReason.APPOINTMENT_HOLD;
import static com.council.appointmentservice.model.AppointmentStatus.*;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final int SLOT_DURATION_HOURS = 1;

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityClient availabilityClient;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            AvailabilityClient availabilityClient
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityClient = availabilityClient;
    }

    /**
     * CLIENT books appointment → slot locked
     */
    @Override
    @Transactional
    public AppointmentResponse createAppointment(
            Long clientId,
            CreateAppointmentRequest request
    ) {

        // 1️⃣ Check if client is already busy at this time
        boolean clientBusy =
                appointmentRepository.existsByClientIdAndAppointmentDateAndStartTimeAndStatusIn(
                        clientId,
                        request.getAppointmentDate(),
                        request.getStartTime(),
                        List.of(CONFIRMED, PENDING_PAYMENT)
                );
        if (clientBusy) {
            throw new IllegalStateException("You already have an appointment at this time");
        }

        // 2️⃣ Check slot availability via Availability Service
        boolean available = availabilityClient.isSlotAvailable(
                request.getCounselorId(),
                request.getAppointmentDate(),
                request.getStartTime()
        );

        if (!available) {
            throw new IllegalStateException("Slot is not available");
        }

        // 3️⃣ Create appointment
        Appointment appointment = new Appointment();
        appointment.setClientId(clientId);
        appointment.setCounselorId(request.getCounselorId());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getStartTime().plusHours(SLOT_DURATION_HOURS));
        appointment.setStatus(PENDING_PAYMENT);
        appointment.setSlotLockedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        // 4️⃣ Block slot via Availability Service
        BlockSlotRequest blockRequest = BlockSlotRequest.builder()
                .counselorId(saved.getCounselorId())
                .date(saved.getAppointmentDate())
                .startTime(saved.getStartTime())
                .reason(APPOINTMENT_HOLD)
                .referenceId(saved.getId())
                .build();
        availabilityClient.blockSlot(blockRequest);

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
    @Transactional
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

        // 1. Check new slot availability
        boolean available = availabilityClient.isSlotAvailable(
                appointment.getCounselorId(),
                request.getNewDate(),
                request.getNewStartTime()
        );
        if (!available) {
            throw new IllegalStateException("New slot is not available");
        }

        // 2. Free old slot
        availabilityClient.freeSlot(appointment.getId());

        // 3. Update appointment to new slot
        appointment.setAppointmentDate(request.getNewDate());
        appointment.setStartTime(request.getNewStartTime());
        appointment.setEndTime(request.getNewStartTime().plusHours(SLOT_DURATION_HOURS));
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        appointment.setSlotLockedAt(LocalDateTime.now());

        Appointment updated = appointmentRepository.save(appointment);

        // 4. Block new slot
        BlockSlotRequest blockRequest = BlockSlotRequest.builder()
                .counselorId(updated.getCounselorId())
                .date(updated.getAppointmentDate())
                .startTime(updated.getStartTime())
                .reason(APPOINTMENT_HOLD) // Or maybe a new RESCHEDULED reason
                .referenceId(updated.getId())
                .build();
        availabilityClient.blockSlot(blockRequest);

        return mapToResponse(updated);
    }

    /**
     * CANCEL appointment
     */
    @Override
    @Transactional
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

        // Free the slot in availability service
        availabilityClient.freeSlot(appointment.getId());
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
