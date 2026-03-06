package com.council.notificationservice.service;

import com.council.notificationservice.dto.SessionNoteShareRequest;

public interface NotificationService {
    void handleSessionNoteShared(Long noteId);

    void handleSessionNoteShared(SessionNoteShareRequest request);
}
