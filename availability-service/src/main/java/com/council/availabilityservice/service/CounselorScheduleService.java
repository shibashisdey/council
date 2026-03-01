package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetLunchBreakRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.dto.response.CounselorScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface CounselorScheduleService {

    void setWorkingHours(Long counselorId, SetWorkingHoursRequest request);

    void setLunchBreak(Long counselorId, SetLunchBreakRequest request);

    void addUnavailability(Long counselorId, AddUnavailabilityRequest request);

    void cancelUnavailability(Long counselorId, Long unavailabilityId);

    List<CounselorAvailabilityResponse> getAvailabilityForDate(Long counselorId, LocalDate date);

    CounselorScheduleResponse getSchedule(Long counselorId);
}
