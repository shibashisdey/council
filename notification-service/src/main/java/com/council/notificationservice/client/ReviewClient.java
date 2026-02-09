package com.council.notificationservice.client;

import com.council.notificationservice.dto.SessionNotePublicResponse;
import com.council.notificationservice.dto.UpdatePdfRequest;

public interface ReviewClient {
    SessionNotePublicResponse getSessionNote(Long noteId);

    void updatePdf(Long noteId, UpdatePdfRequest request);
}
