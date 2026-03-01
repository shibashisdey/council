package com.council.linkgereratorservice.service;

import com.council.linkgereratorservice.dto.CreateMeetingLinkRequest;
import com.council.linkgereratorservice.dto.MeetingLinkResponse;
import com.council.linkgereratorservice.dto.UpdateMeetingLinkRequest;

public interface MeetingLinkService {
    MeetingLinkResponse createOrGet(CreateMeetingLinkRequest request);
    MeetingLinkResponse getByAppointmentId(Long appointmentId);
    MeetingLinkResponse updateMeetingLink(Long appointmentId, UpdateMeetingLinkRequest request);
    void deleteMeetingLink(Long appointmentId);

    String getJoinLink(Long appointmentId);
}
