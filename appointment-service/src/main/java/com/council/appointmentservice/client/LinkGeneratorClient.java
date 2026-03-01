package com.council.appointmentservice.client;

import com.council.appointmentservice.dto.MeetingLinkRequest;
import com.council.appointmentservice.dto.MeetingLinkResponse;
import com.council.appointmentservice.dto.MeetingLinkUpdateRequest;

public interface LinkGeneratorClient {
    MeetingLinkResponse createOrGet(MeetingLinkRequest request);
    MeetingLinkResponse getByAppointmentId(Long appointmentId);
    MeetingLinkResponse updateMeetingLink(Long appointmentId, MeetingLinkUpdateRequest request);
    void deleteMeetingLink(Long appointmentId);
}
