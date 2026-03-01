package com.council.linkgereratorservice.service;

import com.council.linkgereratorservice.dto.CreateMeetingLinkRequest;
import com.council.linkgereratorservice.dto.MeetingLinkResponse;
import com.council.linkgereratorservice.dto.UpdateMeetingLinkRequest;
import com.council.linkgereratorservice.model.MeetingLink;
import com.council.linkgereratorservice.repository.MeetingLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class MeetingLinkServiceImpl implements MeetingLinkService {

    private final MeetingLinkRepository meetingLinkRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String baseUrl;

    public MeetingLinkServiceImpl(
            MeetingLinkRepository meetingLinkRepository,
            @Value("${link.generator.base-url}") String baseUrl
    ) {
        this.meetingLinkRepository = meetingLinkRepository;
        this.baseUrl = baseUrl;
    }

    @Override
    @Transactional
    public MeetingLinkResponse createOrGet(CreateMeetingLinkRequest request) {
        return meetingLinkRepository.findByAppointmentId(request.getAppointmentId())
                .map(this::toResponse)
                .orElseGet(() -> createNew(request));
    }

    @Override
    public MeetingLinkResponse getByAppointmentId(Long appointmentId) {
        MeetingLink link = meetingLinkRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting link not found"));
        return toResponse(link);
    }

    @Override
    @Transactional
    public MeetingLinkResponse updateMeetingLink(Long appointmentId, UpdateMeetingLinkRequest request) {
        MeetingLink link = meetingLinkRepository.findByAppointmentId(appointmentId)
                .orElse(null);

        if (link == null) {
            CreateMeetingLinkRequest create = new CreateMeetingLinkRequest();
            create.setAppointmentId(appointmentId);
            create.setAppointmentDate(request.getAppointmentDate());
            create.setStartTime(request.getStartTime());
            create.setEndTime(request.getEndTime());
            create.setClientId(request.getClientId());
            create.setCounselorId(request.getCounselorId());
            return createNew(create);
        }

        if (request.getClientId() != null) {
            link.setClientId(request.getClientId());
        }
        if (request.getCounselorId() != null) {
            link.setCounselorId(request.getCounselorId());
        }
        if (request.getAppointmentDate() != null) {
            link.setAppointmentDate(request.getAppointmentDate());
        }
        if (request.getStartTime() != null) {
            link.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            link.setEndTime(request.getEndTime());
        }

        MeetingLink saved = meetingLinkRepository.save(link);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMeetingLink(Long appointmentId) {
        meetingLinkRepository.deleteByAppointmentId(appointmentId);
    }

    @Override
    public String getJoinLink(Long appointmentId) {
        MeetingLink link = meetingLinkRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting link not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.of(link.getAppointmentDate(), link.getStartTime());
        LocalDateTime end = LocalDateTime.of(link.getAppointmentDate(), link.getEndTime());

        if (now.isBefore(start) || now.isAfter(end)) {
            throw new IllegalStateException("Meeting link is not available outside the scheduled time");
        }

        return link.getMeetingLink();
    }

    private MeetingLinkResponse createNew(CreateMeetingLinkRequest request) {
        if (request.getAppointmentId() == null) {
            throw new IllegalArgumentException("appointmentId is required");
        }
        if (request.getClientId() == null || request.getCounselorId() == null) {
            throw new IllegalArgumentException("clientId and counselorId are required");
        }
        if (request.getAppointmentDate() == null
                || request.getStartTime() == null
                || request.getEndTime() == null) {
            throw new IllegalArgumentException("appointmentDate, startTime, and endTime are required");
        }

        MeetingLink link = new MeetingLink();
        link.setAppointmentId(request.getAppointmentId());
        link.setClientId(request.getClientId());
        link.setCounselorId(request.getCounselorId());
        link.setAppointmentDate(request.getAppointmentDate());
        link.setStartTime(request.getStartTime());
        link.setEndTime(request.getEndTime());

        String roomName = generateRoomName(request.getAppointmentId());
        link.setRoomName(roomName);
        link.setMeetingLink(buildMeetingLink(roomName));

        MeetingLink saved = meetingLinkRepository.save(link);
        return toResponse(saved);
    }

    private String buildMeetingLink(String roomName) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/" + roomName;
    }

    private String generateRoomName(Long appointmentId) {
        String random = new BigInteger(50, secureRandom).toString(32);
        return "council-" + appointmentId + "-" + random;
    }

    private MeetingLinkResponse toResponse(MeetingLink link) {
        return MeetingLinkResponse.builder()
                .appointmentId(link.getAppointmentId())
                .clientId(link.getClientId())
                .counselorId(link.getCounselorId())
                .appointmentDate(link.getAppointmentDate())
                .startTime(link.getStartTime())
                .endTime(link.getEndTime())
                .meetingLink(link.getMeetingLink())
                .build();
    }
}
