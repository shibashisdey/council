package com.council.notificationservice.service;

import com.council.notificationservice.client.ReviewClient;
import com.council.notificationservice.dto.SessionNotePublicResponse;
import com.council.notificationservice.dto.UpdatePdfRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ReviewClient reviewClient;
    private final R2StorageService r2StorageService;

    public NotificationServiceImpl(
            ReviewClient reviewClient,
            R2StorageService r2StorageService
    ) {
        this.reviewClient = reviewClient;
        this.r2StorageService = r2StorageService;
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

        String content = buildPdfPlaceholderContent(note);
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
    }

    private String buildPdfPlaceholderContent(SessionNotePublicResponse note) {
        return "Session Note\n"
                + "Appointment ID: " + note.getAppointmentId() + "\n"
                + "User ID: " + note.getUserId() + "\n"
                + "Counselor ID: " + note.getCounselorId() + "\n"
                + "Session Date: " + note.getSessionDate() + "\n"
                + "\nSummary:\n" + note.getSummary() + "\n"
                + "\nObservations:\n" + note.getObservations() + "\n"
                + "\nRecommendations:\n" + note.getRecommendations() + "\n";
    }
}
