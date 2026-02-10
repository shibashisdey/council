package com.council.notificationservice.client;

import com.council.notificationservice.dto.AppointmentInternalResponse;

public interface AppointmentClient {
    AppointmentInternalResponse getAppointment(Long appointmentId);
}
