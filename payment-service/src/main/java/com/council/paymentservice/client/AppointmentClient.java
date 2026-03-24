package com.council.paymentservice.client;

import com.council.paymentservice.dto.response.AppointmentInternalResponse;
import com.council.paymentservice.dto.response.AppointmentStatusResponse;

public interface AppointmentClient {
    void confirmAppointment(Long appointmentId) ;

    AppointmentStatusResponse getAppointmentStatus(Long appointmentId);

    AppointmentInternalResponse getAppointmentInternal(Long appointmentId);
}
