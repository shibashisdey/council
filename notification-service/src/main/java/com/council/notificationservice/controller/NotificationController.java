package com.council.notificationservice.controller;

import com.council.notificationservice.service.NotificationService;
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
}
