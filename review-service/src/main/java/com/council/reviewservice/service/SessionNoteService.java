package com.council.reviewservice.service;

import com.council.reviewservice.dto.request.CreateSessionNoteRequest;
import com.council.reviewservice.dto.request.ShareSessionNoteRequest;
import com.council.reviewservice.dto.request.UpdatePdfRequest;
import com.council.reviewservice.dto.request.UpdateSessionNoteRequest;
import com.council.reviewservice.dto.response.SessionNoteClientResponse;
import com.council.reviewservice.dto.response.SessionNoteCounselorResponse;
import com.council.reviewservice.dto.response.SessionNotePublicResponse;

import java.util.List;

public interface SessionNoteService {

    SessionNoteCounselorResponse createSessionNote(Long counselorId, CreateSessionNoteRequest request);

    SessionNoteCounselorResponse updateSessionNote(Long counselorId, Long noteId, UpdateSessionNoteRequest request);

    SessionNoteCounselorResponse shareSessionNote(Long counselorId, Long noteId, ShareSessionNoteRequest request);

    void updatePdf(Long noteId, UpdatePdfRequest request);

    List<SessionNoteClientResponse> getNotesForUser(Long userId);

    List<SessionNoteCounselorResponse> getNotesForCounselor(Long counselorId);

    SessionNotePublicResponse getNoteByAppointmentId(Long appointmentId);

    SessionNotePublicResponse getNoteById(Long noteId);
}
