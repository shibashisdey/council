package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetLunchBreakRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
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

        holidayRepository.findByHolidayDate(date).ifPresent(holiday -> {
            response.add(buildBlock(
                    date,
                    LocalTime.MIN,
                    LocalTime.MAX,
                    "UNAVAILABLE",
                    holiday.getName()
            ));
        });

        lunchBreakRepository.findByCounselorId(counselorId).ifPresent(lunch -> {
            response.add(buildBlock(
                    date,
                    lunch.getStartTime(),
                    lunch.getEndTime(),
                    "UNAVAILABLE",
                    "LUNCH_BREAK"
            ));
        });

        List<CounselorUnavailability> blocks =
                unavailabilityRepository.findByCounselorIdAndDateAndActiveTrue(
                        counselorId,
                        date
                );
        for (CounselorUnavailability block : blocks) {
            response.add(buildBlock(
                    date,
                    block.getStartTime(),
                    block.getEndTime(),
                    "UNAVAILABLE",
                    block.getReason().name()
            ));
        }

        return response;
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
