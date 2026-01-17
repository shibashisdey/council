package com.council.availabilityservice.service;
import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
public interface CounselorScheduleService {

    /**
     * Counselor sets weekly working hours
     */
    void setWorkingHours(
            Long counselorId,
            SetWorkingHoursRequest request
    );

    /**
     * Counselor marks unavailability (leave / lunch / etc.)
     */
    void addUnavailability(
            Long counselorId,
            AddUnavailabilityRequest request
    );

    /**
     * Counselor cancels previously added unavailability
     */
    void cancelUnavailability(
            Long counselorId,
            Long unavailabilityId
    );

    /**
     * Calendar view for counselor / UI
     */
    List<CounselorAvailabilityResponse> getAvailabilityForDate(
            Long counselorId,
            LocalDate date
    );
}
