package com.council.reviewservice.client;

import com.council.reviewservice.dto.response.AppointmentInternalResponse;

public interface AppointmentClient {
    AppointmentInternalResponse getAppointmentInternal(Long appointmentId);

    void completeAppointment(Long appointmentId);
}
