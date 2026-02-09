package com.council.reviewservice.service;

import com.council.reviewservice.client.AppointmentClient;
import com.council.reviewservice.client.NotificationClient;
import com.council.reviewservice.dto.request.CreateSessionNoteRequest;
import com.council.reviewservice.dto.request.ShareSessionNoteRequest;
import com.council.reviewservice.dto.request.UpdatePdfRequest;
import com.council.reviewservice.dto.request.UpdateSessionNoteRequest;
import com.council.reviewservice.dto.response.AppointmentInternalResponse;
import com.council.reviewservice.dto.response.SessionNoteClientResponse;
import com.council.reviewservice.dto.response.SessionNoteCounselorResponse;
import com.council.reviewservice.dto.response.SessionNotePublicResponse;
import com.council.reviewservice.model.SessionNote;
import com.council.reviewservice.repository.SessionNoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SessionNoteServiceImpl implements SessionNoteService {

    private final SessionNoteRepository sessionNoteRepository;
    private final AppointmentClient appointmentClient;
    private final NotificationClient notificationClient;

    public SessionNoteServiceImpl(
            SessionNoteRepository sessionNoteRepository,
            AppointmentClient appointmentClient,
            NotificationClient notificationClient
    ) {
        this.sessionNoteRepository = sessionNoteRepository;
        this.appointmentClient = appointmentClient;
        this.notificationClient = notificationClient;
    }

    @Override
    public SessionNoteCounselorResponse createSessionNote(Long counselorId, CreateSessionNoteRequest request) {
        AppointmentInternalResponse appointment = getAppointmentOrThrow(request.getAppointmentId());

        if (!counselorId.equals(appointment.getCounselorId())) {
            throw new SecurityException("Not allowed to create notes for this appointment");
        }
        if (!"CONFIRMED".equals(appointment.getStatus())
                && !"COMPLETED".equals(appointment.getStatus())) {
            throw new IllegalStateException("Appointment is not in a valid state for notes");
        }

        SessionNote note = new SessionNote();
        note.setAppointmentId(appointment.getAppointmentId());
        note.setUserId(appointment.getClientId());
        note.setCounselorId(appointment.getCounselorId());
        note.setSessionDate(request.getSessionDate() != null
                ? request.getSessionDate()
                : appointment.getAppointmentDate());
        note.setSummary(required(request.getSummary(), "summary"));
        note.setObservations(required(request.getObservations(), "observations"));
        note.setRecommendations(required(request.getRecommendations(), "recommendations"));
        note.setPrivateNotes(request.getPrivateNotes());

        try {
            SessionNote saved = sessionNoteRepository.save(note);
            return toCounselorResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Session note already exists for this appointment", e);
        }
    }

    @Override
    public SessionNoteCounselorResponse updateSessionNote(
            Long counselorId,
            Long noteId,
            UpdateSessionNoteRequest request
    ) {
        SessionNote note = sessionNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Session note not found"));

        if (!counselorId.equals(note.getCounselorId())) {
            throw new SecurityException("Not allowed to update this note");
        }

        if (request.getSummary() != null) {
            note.setSummary(request.getSummary());
        }
        if (request.getObservations() != null) {
            note.setObservations(request.getObservations());
        }
        if (request.getRecommendations() != null) {
            note.setRecommendations(request.getRecommendations());
        }
        if (request.getPrivateNotes() != null) {
            note.setPrivateNotes(request.getPrivateNotes());
        }

        SessionNote saved = sessionNoteRepository.save(note);
        return toCounselorResponse(saved);
    }

    @Override
    public SessionNoteCounselorResponse shareSessionNote(
            Long counselorId,
            Long noteId,
            ShareSessionNoteRequest request
    ) {
        SessionNote note = sessionNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Session note not found"));

        if (!counselorId.equals(note.getCounselorId())) {
            throw new SecurityException("Not allowed to share this note");
        }

        note.setSharedWithClient(request.isShared());
        SessionNote saved = sessionNoteRepository.save(note);

        if (request.isShared()) {
            appointmentClient.completeAppointment(note.getAppointmentId());
            notificationClient.notifySessionNoteShared(note.getId());
        }

        return toCounselorResponse(saved);
    }

    @Override
    public void updatePdf(Long noteId, UpdatePdfRequest request) {
        SessionNote note = sessionNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Session note not found"));

        note.setPdfObjectKey(request.getPdfObjectKey());
        note.setPdfUrl(request.getPdfUrl());
        sessionNoteRepository.save(note);
    }

    @Override
    public List<SessionNoteClientResponse> getNotesForUser(Long userId) {
        return sessionNoteRepository
                .findByUserIdAndSharedWithClientTrueOrderBySessionDateDesc(userId)
                .stream()
                .map(this::toClientResponse)
                .toList();
    }

    @Override
    public List<SessionNoteCounselorResponse> getNotesForCounselor(Long counselorId) {
        return sessionNoteRepository
                .findByCounselorIdOrderBySessionDateDesc(counselorId)
                .stream()
                .map(this::toCounselorResponse)
                .toList();
    }

    @Override
    public SessionNotePublicResponse getNoteByAppointmentId(Long appointmentId) {
        SessionNote note = sessionNoteRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Session note not found"));
        return toPublicResponse(note);
    }

    @Override
    public SessionNotePublicResponse getNoteById(Long noteId) {
        SessionNote note = sessionNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Session note not found"));
        return toPublicResponse(note);
    }

    private AppointmentInternalResponse getAppointmentOrThrow(Long appointmentId) {
        try {
            AppointmentInternalResponse response = appointmentClient.getAppointmentInternal(appointmentId);
            if (response == null) {
                throw new IllegalStateException("Appointment not found");
            }
            return response;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Appointment service unavailable", e);
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private SessionNoteCounselorResponse toCounselorResponse(SessionNote note) {
        return SessionNoteCounselorResponse.builder()
                .id(note.getId())
                .appointmentId(note.getAppointmentId())
                .userId(note.getUserId())
                .counselorId(note.getCounselorId())
                .sessionDate(note.getSessionDate())
                .summary(note.getSummary())
                .observations(note.getObservations())
                .recommendations(note.getRecommendations())
                .privateNotes(note.getPrivateNotes())
                .sharedWithClient(note.isSharedWithClient())
                .pdfObjectKey(note.getPdfObjectKey())
                .pdfUrl(note.getPdfUrl())
                .build();
    }

    private SessionNoteClientResponse toClientResponse(SessionNote note) {
        return SessionNoteClientResponse.builder()
                .id(note.getId())
                .appointmentId(note.getAppointmentId())
                .counselorId(note.getCounselorId())
                .sessionDate(note.getSessionDate())
                .summary(note.getSummary())
                .observations(note.getObservations())
                .recommendations(note.getRecommendations())
                .pdfUrl(note.getPdfUrl())
                .build();
    }

    private SessionNotePublicResponse toPublicResponse(SessionNote note) {
        return SessionNotePublicResponse.builder()
                .id(note.getId())
                .appointmentId(note.getAppointmentId())
                .userId(note.getUserId())
                .counselorId(note.getCounselorId())
                .sessionDate(note.getSessionDate())
                .summary(note.getSummary())
                .observations(note.getObservations())
                .recommendations(note.getRecommendations())
                .pdfObjectKey(note.getPdfObjectKey())
                .pdfUrl(note.getPdfUrl())
                .build();
    }
}
