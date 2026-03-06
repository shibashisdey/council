package com.council.notificationservice.controller;

import com.council.notificationservice.service.NotificationService;
import com.council.notificationservice.dto.SessionNoteShareRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/session-note/{noteId}")
    public ResponseEntity<Void> sessionNoteShared(@PathVariable Long noteId) {
        notificationService.handleSessionNoteShared(noteId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session-note")
    public ResponseEntity<Void> sessionNoteSharedWithContent(@RequestBody SessionNoteShareRequest request) {
        notificationService.handleSessionNoteShared(request);
        return ResponseEntity.ok().build();
    }
}
