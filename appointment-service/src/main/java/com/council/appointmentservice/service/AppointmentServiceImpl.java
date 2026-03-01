package com.council.appointmentservice.service;

import com.council.appointmentservice.client.AvailabilityClient;
import com.council.appointmentservice.client.CounselorClient;
import com.council.appointmentservice.client.LinkGeneratorClient;
import com.council.appointmentservice.dto.BlockSlotRequest;
import com.council.appointmentservice.dto.MeetingLinkRequest;
import com.council.appointmentservice.dto.MeetingLinkResponse;
import com.council.appointmentservice.dto.MeetingLinkUpdateRequest;
import com.council.appointmentservice.dto.request.CreateAppointmentRequest;
import com.council.appointmentservice.dto.request.RescheduleAppointmentRequest;
import com.council.appointmentservice.dto.response.AppointmentInternalResponse;
import com.council.appointmentservice.dto.response.AppointmentResponse;
import com.council.appointmentservice.dto.response.AppointmentStatusResponse;
import com.council.appointmentservice.dto.response.CounselorAppointmentResponse;
import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.model.AppointmentStatus;
import com.council.appointmentservice.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.council.appointmentservice.dto.BlockSlotRequest.UnavailabilityReason.APPOINTMENT_CONFIRMED;
import static com.council.appointmentservice.dto.BlockSlotRequest.UnavailabilityReason.APPOINTMENT_HOLD;
import static com.council.appointmentservice.model.AppointmentStatus.*;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final int SLOT_DURATION_HOURS = 1;
    private static final int BOOKING_WINDOW_DAYS = 45;
    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityClient availabilityClient;
    private final CounselorClient counselorClient;
    private final LinkGeneratorClient linkGeneratorClient;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            AvailabilityClient availabilityClient,
            CounselorClient counselorClient,
            LinkGeneratorClient linkGeneratorClient
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityClient = availabilityClient;
        this.counselorClient = counselorClient;
        this.linkGeneratorClient = linkGeneratorClient;
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
        if (request.getAppointmentDate() == null) {
            throw new IllegalArgumentException("Appointment date is required");
        }
        LocalDate maxDate = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);
        if (request.getAppointmentDate().isAfter(maxDate)) {
            throw new IllegalStateException("Appointments can only be booked up to 45 days in advance");
        }

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
        boolean available = callAvailabilityCheck(
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

        Appointment saved;
        try {
            saved = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Slot already taken", e);
        }

        // 4️⃣ Block slot via Availability Service
        BlockSlotRequest blockRequest = BlockSlotRequest.builder()
                .counselorId(saved.getCounselorId())
                .date(saved.getAppointmentDate())
                .startTime(saved.getStartTime())
                .reason(APPOINTMENT_HOLD)
                .referenceId(saved.getId())
                .build();
        callAvailabilityBlock(blockRequest);

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
                .map(this::attachMeetingLinkIfAvailable)
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
                .map(this::attachMeetingLinkIfAvailable)
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
        if (request.getNewDate() == null) {
            throw new IllegalArgumentException("New appointment date is required");
        }
        LocalDate maxDate = LocalDate.now().plusDays(BOOKING_WINDOW_DAYS);
        if (request.getNewDate().isAfter(maxDate)) {
            throw new IllegalStateException("Appointments can only be booked up to 45 days in advance");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (!appointment.getClientId().equals(clientId)) {
            throw new SecurityException("Not allowed to reschedule this appointment");
        }

        if (appointment.getStatus() == CANCELLED
                || appointment.getStatus() == EXPIRED
                || appointment.getStatus() == COMPLETED) {
            throw new IllegalStateException("This appointment cannot be rescheduled");
        }

        // TODO: enforce 12-hour rule later

        if (appointment.getAppointmentDate().equals(request.getNewDate())
                && appointment.getStartTime().equals(request.getNewStartTime())) {
            return attachMeetingLinkIfAvailable(mapToResponse(appointment));
        }

        // 1. Check new slot availability
        boolean available = callAvailabilityCheck(
                appointment.getCounselorId(),
                request.getNewDate(),
                request.getNewStartTime()
        );
        if (!available) {
            throw new IllegalStateException("New slot is not available");
        }

        // Preserve old slot before changes (for rollback)
        LocalDateTime oldLockedAt = appointment.getSlotLockedAt();
        AppointmentStatus oldStatus = appointment.getStatus();
        var oldDate = appointment.getAppointmentDate();
        var oldStart = appointment.getStartTime();
        var oldEnd = appointment.getEndTime();

        // 2. Free old slot
        callAvailabilityFree(appointment.getId());

        // 3. Block new slot (all-or-nothing)
        BlockSlotRequest.UnavailabilityReason newReason =
                oldStatus == CONFIRMED ? APPOINTMENT_CONFIRMED : APPOINTMENT_HOLD;

        BlockSlotRequest blockRequest = BlockSlotRequest.builder()
                .counselorId(appointment.getCounselorId())
                .date(request.getNewDate())
                .startTime(request.getNewStartTime())
                .reason(newReason)
                .referenceId(appointment.getId())
                .build();
        try {
            callAvailabilityBlock(blockRequest);
        } catch (RuntimeException blockError) {
            // Try to restore old slot
            try {
                BlockSlotRequest restoreRequest = BlockSlotRequest.builder()
                        .counselorId(appointment.getCounselorId())
                        .date(oldDate)
                        .startTime(oldStart)
                        .reason(newReason)
                        .referenceId(appointment.getId())
                        .build();
                callAvailabilityBlock(restoreRequest);
            } catch (RuntimeException restoreError) {
                blockError.addSuppressed(restoreError);
            }
            throw blockError;
        }

        // 4. Update appointment to new slot
        appointment.setAppointmentDate(request.getNewDate());
        appointment.setStartTime(request.getNewStartTime());
        appointment.setEndTime(request.getNewStartTime().plusHours(SLOT_DURATION_HOURS));
        appointment.setStatus(oldStatus);
        appointment.setSlotLockedAt(LocalDateTime.now());

        Appointment updated;
        try {
            updated = appointmentRepository.save(appointment);
        } catch (RuntimeException saveError) {
            // Compensate: free new slot and restore old slot
            try {
                callAvailabilityFree(appointment.getId());
            } catch (RuntimeException freeError) {
                saveError.addSuppressed(freeError);
            }
            try {
                BlockSlotRequest restoreRequest = BlockSlotRequest.builder()
                        .counselorId(appointment.getCounselorId())
                        .date(oldDate)
                        .startTime(oldStart)
                        .reason(newReason)
                        .referenceId(appointment.getId())
                        .build();
                callAvailabilityBlock(restoreRequest);
            } catch (RuntimeException restoreError) {
                saveError.addSuppressed(restoreError);
            }
            appointment.setAppointmentDate(oldDate);
            appointment.setStartTime(oldStart);
            appointment.setEndTime(oldEnd);
            appointment.setSlotLockedAt(oldLockedAt);
            appointment.setStatus(oldStatus);
            throw saveError;
        }

        if (updated.getStatus() == CONFIRMED) {
            updateMeetingLink(updated);
        }

        return attachMeetingLinkIfAvailable(mapToResponse(updated));
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

        boolean isCounselor = false;
        if ("THERAPIST".equals(requesterRole)) {
            var counselor = counselorClient.getCounselorByUserId(requesterId);
            isCounselor = counselor != null
                    && counselor.getId() != null
                    && counselor.getId().equals(appointment.getCounselorId());
        }

        if (!isClient && !isCounselor) {
            throw new SecurityException("Not allowed to cancel this appointment");
        }

        if (appointment.getStatus() == COMPLETED) {
            throw new IllegalStateException("Completed appointments cannot be cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.EXPIRED) {
            callAvailabilityFree(appointment.getId());
            return;
        }

        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
        }

        deleteMeetingLink(appointment.getId());

        // Free the slot in availability service (idempotent)
        callAvailabilityFree(appointment.getId());
    }

    /**
     * PAYMENT SUCCESS -> Confirm appointment
     */
    @Override
    @Transactional
    public AppointmentResponse confirmAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appointment.getStatus() == CONFIRMED) {
            callAvailabilityUpdateReason(appointment.getId(), APPOINTMENT_CONFIRMED);
            ensureMeetingLink(appointment);
            return attachMeetingLinkIfAvailable(mapToResponse(appointment));
        }

        if (appointment.getStatus() != PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending appointments can be confirmed");
        }

        appointment.setStatus(CONFIRMED);
        Appointment updated = appointmentRepository.save(appointment);

        callAvailabilityUpdateReason(updated.getId(), APPOINTMENT_CONFIRMED);

        ensureMeetingLink(updated);
        return attachMeetingLinkIfAvailable(mapToResponse(updated));
    }

    @Override
    public AppointmentStatusResponse getAppointmentStatus(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        return AppointmentStatusResponse.builder()
                .appointmentId(appointment.getId())
                .status(appointment.getStatus())
                .build();
    }

    @Override
    public AppointmentInternalResponse getAppointmentInternal(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        return AppointmentInternalResponse.builder()
                .appointmentId(appointment.getId())
                .clientId(appointment.getClientId())
                .counselorId(appointment.getCounselorId())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .build();
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appointment.getStatus() == COMPLETED) {
            return mapToResponse(appointment);
        }

        if (appointment.getStatus() != CONFIRMED) {
            throw new IllegalStateException("Only confirmed appointments can be completed");
        }

        appointment.setStatus(COMPLETED);
        Appointment updated = appointmentRepository.save(appointment);
        return attachMeetingLinkIfAvailable(mapToResponse(updated));
    }

    private void callAvailabilityBlock(BlockSlotRequest request) {
        try {
            availabilityClient.blockSlot(request);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Availability service unavailable", e);
        }
    }

    private void callAvailabilityFree(Long referenceId) {
        try {
            availabilityClient.freeSlot(referenceId);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Availability service unavailable", e);
        }
    }

    private void callAvailabilityUpdateReason(Long referenceId, BlockSlotRequest.UnavailabilityReason reason) {
        try {
            availabilityClient.updateBlockReason(referenceId, reason);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Availability service unavailable", e);
        }
    }

    private boolean callAvailabilityCheck(Long counselorId, java.time.LocalDate date, java.time.LocalTime startTime) {
        try {
            return availabilityClient.isSlotAvailable(counselorId, date, startTime);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Availability service unavailable", e);
        }
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

    private AppointmentResponse attachMeetingLinkIfAvailable(AppointmentResponse response) {
        if (!shouldExposeMeetingLink(
                response.getStatus(),
                response.getAppointmentDate(),
                response.getEndTime()
        )) {
            return response;
        }

        String link = getMeetingLinkSafely(response.getAppointmentId());
        if (link == null) {
            return response;
        }

        return AppointmentResponse.builder()
                .appointmentId(response.getAppointmentId())
                .clientId(response.getClientId())
                .clientName(response.getClientName())
                .counselorId(response.getCounselorId())
                .counselorName(response.getCounselorName())
                .appointmentDate(response.getAppointmentDate())
                .startTime(response.getStartTime())
                .endTime(response.getEndTime())
                .status(response.getStatus())
                .paymentId(response.getPaymentId())
                .createdAt(response.getCreatedAt())
                .meetingLink(link)
                .build();
    }

    private CounselorAppointmentResponse attachMeetingLinkIfAvailable(CounselorAppointmentResponse response) {
        if (!shouldExposeMeetingLink(
                response.getStatus(),
                response.getAppointmentDate(),
                response.getEndTime()
        )) {
            return response;
        }

        String link = getMeetingLinkSafely(response.getAppointmentId());
        if (link == null) {
            return response;
        }

        return CounselorAppointmentResponse.builder()
                .appointmentId(response.getAppointmentId())
                .clientId(response.getClientId())
                .appointmentDate(response.getAppointmentDate())
                .startTime(response.getStartTime())
                .endTime(response.getEndTime())
                .status(response.getStatus())
                .meetingLink(link)
                .build();
    }

    private boolean shouldExposeMeetingLink(
            AppointmentStatus status,
            LocalDate appointmentDate,
            LocalTime endTime
    ) {
        if (status != CONFIRMED) {
            return false;
        }
        if (appointmentDate == null || endTime == null) {
            return false;
        }
        LocalDateTime appointmentEnd = LocalDateTime.of(appointmentDate, endTime);
        return !appointmentEnd.isBefore(LocalDateTime.now());
    }

    private String getMeetingLinkSafely(Long appointmentId) {
        try {
            MeetingLinkResponse response = linkGeneratorClient.getByAppointmentId(appointmentId);
            return response != null ? response.getMeetingLink() : null;
        } catch (RuntimeException e) {
            log.warn("Meeting link fetch failed for appointment {}", appointmentId, e);
            return null;
        }
    }

    private void ensureMeetingLink(Appointment appointment) {
        try {
            MeetingLinkRequest request = MeetingLinkRequest.builder()
                    .appointmentId(appointment.getId())
                    .clientId(appointment.getClientId())
                    .counselorId(appointment.getCounselorId())
                    .appointmentDate(appointment.getAppointmentDate())
                    .startTime(appointment.getStartTime())
                    .endTime(appointment.getEndTime())
                    .build();
            linkGeneratorClient.createOrGet(request);
        } catch (RuntimeException e) {
            log.warn("Meeting link generation failed for appointment {}", appointment.getId(), e);
        }
    }

    private void updateMeetingLink(Appointment appointment) {
        try {
            MeetingLinkUpdateRequest request = MeetingLinkUpdateRequest.builder()
                    .clientId(appointment.getClientId())
                    .counselorId(appointment.getCounselorId())
                    .appointmentDate(appointment.getAppointmentDate())
                    .startTime(appointment.getStartTime())
                    .endTime(appointment.getEndTime())
                    .build();
            linkGeneratorClient.updateMeetingLink(appointment.getId(), request);
        } catch (RuntimeException e) {
            log.warn("Meeting link update failed for appointment {}", appointment.getId(), e);
        }
    }

    private void deleteMeetingLink(Long appointmentId) {
        try {
            linkGeneratorClient.deleteMeetingLink(appointmentId);
        } catch (RuntimeException e) {
            log.warn("Meeting link delete failed for appointment {}", appointmentId, e);
        }
    }
}
