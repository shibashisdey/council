package com.council.appointmentservice.model;

public enum AppointmentStatus {

    PENDING_PAYMENT,   // Slot held (10 mins)
    CONFIRMED,         // Payment successful
    CANCELLED,         // Cancelled by client or counselor
    RESCHEDULED,       // Rescheduled to another slot
    COMPLETED,         // Session done
    EXPIRED            // Payment not completed in 10 mins
}
