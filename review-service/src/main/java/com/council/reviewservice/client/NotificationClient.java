package com.council.reviewservice.client;

import com.council.reviewservice.dto.request.NotifySessionNoteRequest;

public interface NotificationClient {
    void notifySessionNoteShared(Long sessionNoteId);

    void notifySessionNoteSharedWithContent(NotifySessionNoteRequest request);
}
