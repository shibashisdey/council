package com.council.appointmentservice.service;

import com.council.appointmentservice.dto.request.CreateAppointmentRequest;
import com.council.appointmentservice.dto.request.RescheduleAppointmentRequest;
import com.council.appointmentservice.dto.response.AppointmentResponse;
import com.council.appointmentservice.dto.response.CounselorAppointmentResponse;

import java.util.List;

public interface AppointmentService {

    /**
     * Client books an appointment (slot gets locked for payment)
     */
    AppointmentResponse createAppointment(
            Long clientId,
            CreateAppointmentRequest request
    );

    /**
     * Client views his appointments
     */
    List<AppointmentResponse> getAppointmentsForClient(Long clientId);

    /**
     * Counselor views his appointments
     */
    List<CounselorAppointmentResponse> getAppointmentsForCounselor(Long counselorId);

    /**
     * Client requests reschedule
     */
    AppointmentResponse rescheduleAppointment(
            Long appointmentId,
            Long clientId,
            RescheduleAppointmentRequest request
    );

    /**
     * Cancel appointment (client or counselor)
     */
    void cancelAppointment(
            Long appointmentId,
            Long requesterId,
            String requesterRole
    );
}
