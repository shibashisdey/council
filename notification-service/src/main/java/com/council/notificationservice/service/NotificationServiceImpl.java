package com.council.notificationservice.service;

import com.council.notificationservice.client.AppointmentClient;
import com.council.notificationservice.client.CounselorClient;
import com.council.notificationservice.client.ReviewClient;
import com.council.notificationservice.client.UserClient;
import com.council.notificationservice.dto.AppointmentInternalResponse;
import com.council.notificationservice.dto.CounselorResponse;
import com.council.notificationservice.dto.SessionNotePublicResponse;
import com.council.notificationservice.dto.SessionNoteShareRequest;
import com.council.notificationservice.dto.UserPublicResponse;
import com.council.notificationservice.dto.UpdatePdfRequest;
import com.council.notificationservice.messaging.EmailEventPublisher;
import com.council.notificationservice.messaging.EmailNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ReviewClient reviewClient;
    private final AppointmentClient appointmentClient;
    private final UserClient userClient;
    private final CounselorClient counselorClient;
    private final R2StorageService r2StorageService;
    private final EmailEventPublisher emailEventPublisher;

    public NotificationServiceImpl(
            ReviewClient reviewClient,
            AppointmentClient appointmentClient,
            UserClient userClient,
            CounselorClient counselorClient,
            R2StorageService r2StorageService,
            EmailEventPublisher emailEventPublisher
    ) {
        this.reviewClient = reviewClient;
        this.appointmentClient = appointmentClient;
        this.userClient = userClient;
        this.counselorClient = counselorClient;
        this.r2StorageService = r2StorageService;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    public void handleSessionNoteShared(Long noteId) {
        log.info("Handling session note share: noteId={}", noteId);
        SessionNotePublicResponse note;
        try {
            note = reviewClient.getSessionNote(noteId);
        } catch (IllegalArgumentException e) {
            log.warn("Session note not found, skipping: noteId={}", noteId);
            return;
        }

        String objectKey = "session-notes/"
                + note.getUserId() + "/"
                + note.getAppointmentId() + ".pdf";

        AppointmentInternalResponse appointment = appointmentClient.getAppointment(note.getAppointmentId());
        UserPublicResponse user = userClient.getUserPublic(note.getUserId());
        CounselorResponse counselor = counselorClient.getCounselorById(note.getCounselorId());

        String content = buildPdfPlaceholderContent(note, appointment, user, counselor);
        String publicUrl;
        try {
            publicUrl = r2StorageService.uploadPdfPlaceholder(objectKey, content);
            log.info("Uploaded PDF to R2: noteId={}, objectKey={}, url={}", noteId, objectKey, publicUrl);
        } catch (RuntimeException e) {
            log.error("Failed to upload PDF to R2: noteId={}, objectKey={}", noteId, objectKey, e);
            throw e;
        }

        UpdatePdfRequest update = new UpdatePdfRequest();
        update.setPdfObjectKey(objectKey);
        update.setPdfUrl(publicUrl);
        try {
            reviewClient.updatePdf(noteId, update);
            log.info("Updated review service with PDF info: noteId={}", noteId);
        } catch (RuntimeException e) {
            log.error("Failed to update review service with PDF info: noteId={}", noteId, e);
            throw e;
        }
        publishSessionNoteSharedEvent(appointment, publicUrl);
    }

    @Override
    public void handleSessionNoteShared(SessionNoteShareRequest request) {
        if (request == null || request.getNoteId() == null || request.getAppointmentId() == null) {
            throw new IllegalArgumentException("noteId and appointmentId are required");
        }
        Long noteId = request.getNoteId();
        log.info("Handling session note share with content: noteId={}, appointmentId={}",
                noteId, request.getAppointmentId());

        AppointmentInternalResponse appointment = appointmentClient.getAppointment(request.getAppointmentId());
        UserPublicResponse user = userClient.getUserPublic(appointment.getClientId());
        CounselorResponse counselor = counselorClient.getCounselorById(appointment.getCounselorId());

        String objectKey = "session-notes/"
                + appointment.getClientId() + "/"
                + appointment.getAppointmentId() + ".pdf";

        String content = buildPdfPlaceholderContent(
                toSessionNoteResponse(request, appointment),
                appointment,
                user,
                counselor
        );
        String publicUrl;
        try {
            publicUrl = r2StorageService.uploadPdfPlaceholder(objectKey, content);
            log.info("Uploaded PDF to R2: noteId={}, objectKey={}, url={}", noteId, objectKey, publicUrl);
        } catch (RuntimeException e) {
            log.error("Failed to upload PDF to R2: noteId={}, objectKey={}", noteId, objectKey, e);
            throw e;
        }

        UpdatePdfRequest update = new UpdatePdfRequest();
        update.setPdfObjectKey(objectKey);
        update.setPdfUrl(publicUrl);
        try {
            reviewClient.updatePdf(noteId, update);
            log.info("Updated review service with PDF info: noteId={}", noteId);
        } catch (RuntimeException e) {
            log.error("Failed to update review service with PDF info: noteId={}", noteId, e);
            throw e;
        }
        publishSessionNoteSharedEvent(appointment, publicUrl);
    }

    private String buildPdfPlaceholderContent(
            SessionNotePublicResponse note,
            AppointmentInternalResponse appointment,
            UserPublicResponse user,
            CounselorResponse counselor
    ) {
        String counselorName = counselor != null && counselor.getFullName() != null
                ? counselor.getFullName()
                : "Counselor";
        String userName = user != null && user.getFullName() != null
                ? user.getFullName()
                : "Client";
        String date = appointment != null && appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().toString()
                : String.valueOf(note.getSessionDate());
        String time = appointment != null && appointment.getStartTime() != null
                ? appointment.getStartTime() + " - " + appointment.getEndTime()
                : "N/A";

        return "Council Session Note\n"
                + "\nCounselor: " + counselorName + "\n"
                + "Client: " + userName + "\n"
                + "Date: " + date + "\n"
                + "Time: " + time + "\n"
                + "\nSummary:\n" + safe(note.getSummary()) + "\n"
                + "\nObservations:\n" + safe(note.getObservations()) + "\n"
                + "\nRecommendations:\n" + safe(note.getRecommendations()) + "\n";
    }

    private SessionNotePublicResponse toSessionNoteResponse(
            SessionNoteShareRequest request,
            AppointmentInternalResponse appointment
    ) {
        SessionNotePublicResponse response = new SessionNotePublicResponse();
        response.setId(request.getNoteId());
        response.setAppointmentId(request.getAppointmentId());
        response.setUserId(appointment.getClientId());
        response.setCounselorId(appointment.getCounselorId());
        response.setSessionDate(appointment.getAppointmentDate());
        response.setSummary(request.getSummary());
        response.setObservations(request.getObservations());
        response.setRecommendations(request.getRecommendations());
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void publishSessionNoteSharedEvent(AppointmentInternalResponse appointment, String pdfUrl) {
        if (appointment == null) {
            return;
        }
        emailEventPublisher.publish(EmailNotificationEvent.builder()
                .eventType("SESSION_NOTE_SHARED")
                .occurredAt(java.time.Instant.now())
                .appointmentId(appointment.getAppointmentId())
                .clientUserId(appointment.getClientId())
                .counselorId(appointment.getCounselorId())
                .appointmentDate(appointment.getAppointmentDate() != null ? appointment.getAppointmentDate().toString() : null)
                .startTime(appointment.getStartTime() != null ? appointment.getStartTime().toString() : null)
                .endTime(appointment.getEndTime() != null ? appointment.getEndTime().toString() : null)
                .pdfUrl(pdfUrl)
                .build());
    }
}
