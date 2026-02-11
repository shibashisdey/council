package com.council.appointmentservice.controller;

import com.council.appointmentservice.client.CounselorClient;
import com.council.appointmentservice.dto.request.CreateAppointmentRequest;
import com.council.appointmentservice.dto.request.RescheduleAppointmentRequest;
import com.council.appointmentservice.dto.response.AppointmentInternalResponse;
import com.council.appointmentservice.dto.response.AppointmentResponse;
import com.council.appointmentservice.dto.response.AppointmentStatusResponse;
import com.council.appointmentservice.dto.response.CounselorAppointmentResponse;
import com.council.appointmentservice.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CounselorClient counselorClient;

    public AppointmentController(AppointmentService appointmentService, CounselorClient counselorClient) {
        this.appointmentService = appointmentService;
        this.counselorClient = counselorClient;
    }

    /**
     * CLIENT → Book appointment (slot locked)
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @RequestHeader("X-USER-ID") Long clientId,
            @RequestBody CreateAppointmentRequest request
    ) {
        return new ResponseEntity<>(
                appointmentService.createAppointment(clientId, request),
                HttpStatus.CREATED
        );
    }

    /**
     * CLIENT → View own appointments
     */
    @GetMapping("/client")
    public List<AppointmentResponse> getClientAppointments(
            @RequestHeader("X-USER-ID") Long clientId
    ) {
        return appointmentService.getAppointmentsForClient(clientId);
    }

    /**
     * COUNSELOR → View own appointments
     */
    @GetMapping("/counselor/{counselorId}")
    public List<CounselorAppointmentResponse> getCounselorAppointments(
            @RequestHeader("X-USER-ID") Long requesterId,
            @RequestHeader("X-USER-ROLE") String role,
            @PathVariable Long counselorId
    ) {
        requireTherapist(role);
        var counselor = counselorClient.getCounselorByUserId(requesterId);
        if (counselor == null || counselor.getId() == null || !counselor.getId().equals(counselorId)) {
            throw new SecurityException("Not allowed to access these appointments");
        }
        return appointmentService.getAppointmentsForCounselor(counselorId);
    }

    /**
     * CLIENT → Reschedule appointment
     */
    @PutMapping("/{appointmentId}/reschedule")
    public AppointmentResponse rescheduleAppointment(
            @PathVariable Long appointmentId,
            @RequestHeader("X-USER-ID") Long clientId,
            @RequestBody RescheduleAppointmentRequest request
    ) {
        return appointmentService.rescheduleAppointment(
                appointmentId, clientId, request
        );
    }

    /**
     * CLIENT / COUNSELOR → Cancel appointment
     */
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable Long appointmentId,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE") String role
    ) {
        appointmentService.cancelAppointment(appointmentId, userId, role);
        return ResponseEntity.noContent().build();
    }

    /**
     * INTERNAL -> Confirm appointment after payment success
     */
    @PutMapping("/{appointmentId}/confirm")
    public AppointmentResponse confirmAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.confirmAppointment(appointmentId);
    }

    /**
     * INTERNAL -> Get appointment status
     */
    @GetMapping("/{appointmentId}/status")
    public AppointmentStatusResponse getAppointmentStatus(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.getAppointmentStatus(appointmentId);
    }

    /**
     * INTERNAL -> Get appointment details for other services
     */
    @GetMapping("/{appointmentId}/internal")
    public AppointmentInternalResponse getAppointmentInternal(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.getAppointmentInternal(appointmentId);
    }

    /**
     * INTERNAL -> Complete appointment after session
     */
    @PutMapping("/{appointmentId}/complete")
    public AppointmentResponse completeAppointment(
            @PathVariable Long appointmentId
    ) {
        return appointmentService.completeAppointment(appointmentId);
    }

    private void requireTherapist(String role) {
        if (!"THERAPIST".equals(role)) {
            throw new SecurityException("Therapist access only");
        }
    }
}
