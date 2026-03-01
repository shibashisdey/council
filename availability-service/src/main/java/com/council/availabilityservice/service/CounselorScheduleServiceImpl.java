package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetLunchBreakRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.dto.response.CounselorScheduleResponse;
import com.council.availabilityservice.dto.response.LunchBreakResponse;
import com.council.availabilityservice.dto.response.WorkingHoursResponse;
import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.LunchBreakRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CounselorScheduleServiceImpl implements CounselorScheduleService {

    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final LunchBreakRepository lunchBreakRepository;
    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final PublicHolidayRepository holidayRepository;

    public CounselorScheduleServiceImpl(
            CounselorWorkingHoursRepository workingHoursRepository,
            LunchBreakRepository lunchBreakRepository,
            CounselorUnavailabilityRepository unavailabilityRepository,
            PublicHolidayRepository holidayRepository
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.lunchBreakRepository = lunchBreakRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public void setWorkingHours(Long counselorId, SetWorkingHoursRequest request) {
        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(
                                counselorId,
                                request.getDayOfWeek()
                        )
                        .orElse(new CounselorWorkingHours());

        workingHours.setCounselorId(counselorId);
        workingHours.setDayOfWeek(request.getDayOfWeek());
        workingHours.setStartTime(request.getStartTime());
        workingHours.setEndTime(request.getEndTime());

        workingHoursRepository.save(workingHours);
    }

    @Override
    public void setLunchBreak(Long counselorId, SetLunchBreakRequest request) {
        LunchBreak lunchBreak =
                lunchBreakRepository.findByCounselorId(counselorId)
                        .orElse(new LunchBreak());

        lunchBreak.setCounselorId(counselorId);
        lunchBreak.setStartTime(request.getStartTime());
        lunchBreak.setEndTime(request.getEndTime());

        lunchBreakRepository.save(lunchBreak);
    }

    @Override
    public void addUnavailability(Long counselorId, AddUnavailabilityRequest request) {
        CounselorUnavailability unavailability = new CounselorUnavailability();
        unavailability.setCounselorId(counselorId);
        unavailability.setDate(request.getDate());
        unavailability.setStartTime(request.getStartTime());
        unavailability.setEndTime(request.getEndTime());
        unavailability.setReason(request.getReason());
        unavailabilityRepository.save(unavailability);
    }

    @Override
    public void cancelUnavailability(Long counselorId, Long unavailabilityId) {
        CounselorUnavailability unavailability =
                unavailabilityRepository.findById(unavailabilityId)
                        .orElseThrow(() -> new IllegalArgumentException("Unavailability not found"));

        if (!unavailability.getCounselorId().equals(counselorId)) {
            throw new SecurityException("Not allowed to cancel this unavailability");
        }

        unavailability.setActive(false);
        unavailabilityRepository.save(unavailability);
    }

    @Override
    public List<CounselorAvailabilityResponse> getAvailabilityForDate(Long counselorId, LocalDate date) {
        List<CounselorAvailabilityResponse> response = new ArrayList<>();

        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(
                                counselorId,
                                date.getDayOfWeek()
                        )
                        .orElse(null);

        if (workingHours == null) {
            return response;
        }

        var holidayOpt = holidayRepository.findByHolidayDate(date);
        String holidayReason = holidayOpt.map(h -> h.getName()).orElse(null);

        List<TimeBlock> blocked = new ArrayList<>();
        lunchBreakRepository.findByCounselorId(counselorId).ifPresent(lunch ->
                blocked.add(new TimeBlock(lunch.getStartTime(), lunch.getEndTime(), "LUNCH_BREAK"))
        );

        List<CounselorUnavailability> blocks =
                unavailabilityRepository.findByCounselorIdAndDateAndActiveTrue(
                        counselorId,
                        date
                );
        for (CounselorUnavailability block : blocks) {
            blocked.add(new TimeBlock(
                    block.getStartTime(),
                    block.getEndTime(),
                    block.getReason().name()
            ));
        }

        LocalTime slotStart = workingHours.getStartTime();
        while (!slotStart.plusHours(1).isAfter(workingHours.getEndTime())) {
            LocalTime slotEnd = slotStart.plusHours(1);
            if (holidayReason != null) {
                response.add(buildBlock(date, slotStart, slotEnd, "UNAVAILABLE", holidayReason));
            } else {
                String reason = findOverlapReason(blocked, slotStart, slotEnd);
                if (reason == null) {
                    response.add(buildBlock(date, slotStart, slotEnd, "AVAILABLE", ""));
                } else {
                    response.add(buildBlock(date, slotStart, slotEnd, "UNAVAILABLE", reason));
                }
            }
            slotStart = slotStart.plusHours(1);
        }

        return response;
    }

    @Override
    public CounselorScheduleResponse getSchedule(Long counselorId) {
        List<WorkingHoursResponse> workingHours = workingHoursRepository.findByCounselorId(counselorId)
                .stream()
                .map(hours -> WorkingHoursResponse.builder()
                        .dayOfWeek(hours.getDayOfWeek())
                        .startTime(hours.getStartTime())
                        .endTime(hours.getEndTime())
                        .build())
                .collect(Collectors.toList());

        LunchBreakResponse lunchBreak = lunchBreakRepository.findByCounselorId(counselorId)
                .map(breakTime -> LunchBreakResponse.builder()
                        .startTime(breakTime.getStartTime())
                        .endTime(breakTime.getEndTime())
                        .build())
                .orElse(null);

        return CounselorScheduleResponse.builder()
                .workingHours(workingHours)
                .lunchBreak(lunchBreak)
                .build();
    }

    private String findOverlapReason(List<TimeBlock> blocks, LocalTime start, LocalTime end) {
        for (TimeBlock block : blocks) {
            if (block.start.isBefore(end) && block.end.isAfter(start)) {
                return block.reason;
            }
        }
        return null;
    }

    private static class TimeBlock {
        private final LocalTime start;
        private final LocalTime end;
        private final String reason;

        private TimeBlock(LocalTime start, LocalTime end, String reason) {
            this.start = start;
            this.end = end;
            this.reason = reason;
        }
    }

    private CounselorAvailabilityResponse buildBlock(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String status,
            String reason
    ) {
        return CounselorAvailabilityResponse.builder()
                .date(date)
                .startTime(start)
                .endTime(end)
                .status(status)
                .reason(reason)
                .build();
    }
}
