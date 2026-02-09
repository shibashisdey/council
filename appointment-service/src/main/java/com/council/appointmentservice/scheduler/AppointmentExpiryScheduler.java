package com.council.appointmentservice.scheduler;

import com.council.appointmentservice.client.AvailabilityClient;
import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.model.AppointmentStatus;
import com.council.appointmentservice.repository.AppointmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AppointmentExpiryScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilityClient availabilityClient;

    public AppointmentExpiryScheduler(
            AppointmentRepository appointmentRepository,
            AvailabilityClient availabilityClient
    ) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityClient = availabilityClient;
    }

    /**
     * Runs every 1 minute
     * Expires appointments stuck in PENDING_PAYMENT for more than 10 mins
     */
    @Scheduled(fixedRate = 60000)
    public void expireUnpaidAppointments() {

        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(10);

        List<Appointment> expiredAppointments =
                appointmentRepository.findByStatusAndSlotLockedAtBefore(
                        AppointmentStatus.PENDING_PAYMENT,
                        expiryTime
                );

        for (Appointment appointment : expiredAppointments) {
            appointment.setStatus(AppointmentStatus.EXPIRED);
            availabilityClient.freeSlot(appointment.getId());
        }

        appointmentRepository.saveAll(expiredAppointments);
    }
}
