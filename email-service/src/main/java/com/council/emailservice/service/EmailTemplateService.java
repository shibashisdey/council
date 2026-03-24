package com.council.emailservice.service;

import com.council.emailservice.dto.EmailNotificationEvent;
import com.council.emailservice.dto.RenderedEmail;
import com.council.emailservice.dto.ResolvedAppointmentContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmailTemplateService {

    public List<RenderedEmail> render(EmailNotificationEvent event, ResolvedAppointmentContext ctx) {
        return switch (event.getEventType()) {
            case "USER_REGISTERED" -> List.of(RenderedEmail.builder()
                    .to(event.getUserEmail())
                    .subject("Welcome to StackFul Minds")
                    .body("Your account has been created successfully as " + safe(event.getRole()) + ".")
                    .build());
            case "APPOINTMENT_CREATED" -> appointmentCreated(ctx);
            case "PAYMENT_CONFIRMED" -> paymentConfirmed(ctx, event);
            case "PAYMENT_FAILED" -> paymentFailed(ctx, event);
            case "APPOINTMENT_CANCELLED" -> appointmentCancelled(ctx, event);
            case "APPOINTMENT_RESCHEDULE_REQUESTED" -> rescheduleRequested(ctx);
            case "APPOINTMENT_RESCHEDULED" -> rescheduled(ctx);
            case "APPOINTMENT_RESCHEDULE_REJECTED" -> rescheduleRejected(ctx);
            case "SESSION_NOTE_SHARED" -> sessionNoteShared(ctx, event);
            default -> List.of();
        };
    }

    private List<RenderedEmail> appointmentCreated(ResolvedAppointmentContext ctx) {
        List<RenderedEmail> emails = new ArrayList<>();
        emails.add(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Appointment reserved")
                .body("Your appointment has been reserved for " + slot(ctx) + ". Complete payment to confirm it.")
                .build());
        emails.add(RenderedEmail.builder()
                .to(ctx.getCounselorEmail())
                .subject("New appointment hold")
                .body("A client has reserved a slot for " + slot(ctx) + ". It will be confirmed after payment.")
                .build());
        return emails;
    }

    private List<RenderedEmail> paymentConfirmed(ResolvedAppointmentContext ctx, EmailNotificationEvent event) {
        List<RenderedEmail> emails = new ArrayList<>();
        String amount = event.getAmount() != null ? event.getAmount().toPlainString() : "the requested amount";
        emails.add(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Appointment confirmed")
                .body("Your payment of " + amount + " was received. Your session is confirmed for " + slot(ctx) + ".")
                .build());
        emails.add(RenderedEmail.builder()
                .to(ctx.getCounselorEmail())
                .subject("Appointment confirmed")
                .body("A client payment was received. The session is confirmed for " + slot(ctx) + ".")
                .build());
        return emails;
    }

    private List<RenderedEmail> paymentFailed(ResolvedAppointmentContext ctx, EmailNotificationEvent event) {
        String amount = event.getAmount() != null ? event.getAmount().toPlainString() : "the requested amount";
        return List.of(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Payment failed")
                .body("Payment of " + amount + " failed for your reserved appointment on " + slot(ctx) + ".")
                .build());
    }

    private List<RenderedEmail> appointmentCancelled(ResolvedAppointmentContext ctx, EmailNotificationEvent event) {
        String actor = "THERAPIST".equals(event.getActorRole()) ? "The therapist" : "The client";
        List<RenderedEmail> emails = new ArrayList<>();
        emails.add(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Appointment cancelled")
                .body(actor + " cancelled the session scheduled for " + slot(ctx) + ".")
                .build());
        emails.add(RenderedEmail.builder()
                .to(ctx.getCounselorEmail())
                .subject("Appointment cancelled")
                .body(actor + " cancelled the session scheduled for " + slot(ctx) + ".")
                .build());
        return emails;
    }

    private List<RenderedEmail> rescheduleRequested(ResolvedAppointmentContext ctx) {
        return List.of(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Reschedule request received")
                .body("The therapist requested a reschedule for your appointment currently set for " + slot(ctx) + ".")
                .build());
    }

    private List<RenderedEmail> rescheduled(ResolvedAppointmentContext ctx) {
        List<RenderedEmail> emails = new ArrayList<>();
        emails.add(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Appointment rescheduled")
                .body("Your appointment has been rescheduled to " + slot(ctx) + ".")
                .build());
        emails.add(RenderedEmail.builder()
                .to(ctx.getCounselorEmail())
                .subject("Appointment rescheduled")
                .body("An appointment has been rescheduled to " + slot(ctx) + ".")
                .build());
        return emails;
    }

    private List<RenderedEmail> rescheduleRejected(ResolvedAppointmentContext ctx) {
        return List.of(RenderedEmail.builder()
                .to(ctx.getCounselorEmail())
                .subject("Reschedule request rejected")
                .body("The client rejected the reschedule request for the appointment on " + slot(ctx) + ".")
                .build());
    }

    private List<RenderedEmail> sessionNoteShared(ResolvedAppointmentContext ctx, EmailNotificationEvent event) {
        String body = "Your therapist shared session notes for " + slot(ctx) + ".";
        if (event.getPdfUrl() != null && !event.getPdfUrl().isBlank()) {
            body += "\n\nDownload: " + event.getPdfUrl();
        }
        return List.of(RenderedEmail.builder()
                .to(ctx.getClientEmail())
                .subject("Session notes shared")
                .body(body)
                .build());
    }

    private String slot(ResolvedAppointmentContext ctx) {
        return safe(ctx.getAppointmentDate()) + " " + safe(ctx.getStartTime()) + " - " + safe(ctx.getEndTime());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
