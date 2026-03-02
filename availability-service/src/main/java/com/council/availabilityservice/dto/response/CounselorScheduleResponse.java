package com.council.availabilityservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CounselorScheduleResponse {

    private List<WorkingHoursResponse> workingHours;

    private List<LunchBreakResponse> lunchBreaks;

    private LunchBreakResponse lunchBreak;
}
