package com.council.availabilityservice.service;
import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.LunchBreakRepository;
import com.council.availabilityservice.service.CounselorScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CounselorScheduleServiceImpl implements CounselorScheduleService {

    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final LunchBreakRepository lunchBreakRepository;

    public CounselorScheduleServiceImpl(
            CounselorWorkingHoursRepository workingHoursRepository,
            CounselorUnavailabilityRepository unavailabilityRepository,
            LunchBreakRepository lunchBreakRepository
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.lunchBreakRepository = lunchBreakRepository;
    }

    /**
     * Counselor sets weekly working hours
     */
    @Override
    public void setWorkingHours(
            Long counselorId,
            SetWorkingHoursRequest request
    ) {

        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, request.getDayOfWeek())
                        .orElse(new CounselorWorkingHours());

        workingHours.setCounselorId(counselorId);
        workingHours.setDayOfWeek(request.getDayOfWeek());
        workingHours.setStartTime(request.getStartTime());
        workingHours.setEndTime(request.getEndTime());

        workingHoursRepository.save(workingHours);
    }

    /**
     * Counselor marks unavailability
     */
    @Override
    public void addUnavailability(
            Long counselorId,
            AddUnavailabilityRequest request
    ) {

        CounselorUnavailability unavailability = new CounselorUnavailability();
        unavailability.setCounselorId(counselorId);
        unavailability.setDate(request.getDate());
        unavailability.setStartTime(request.getStartTime());
        unavailability.setEndTime(request.getEndTime());
        unavailability.setReason(
                UnavailabilityReason.valueOf(request.getReason())
        );

        unavailabilityRepository.save(unavailability);
    }

    /**
     * Counselor cancels previously added unavailability
     */
    @Override
    public void cancelUnavailability(
            Long counselorId,
            Long unavailabilityId
    ) {

        CounselorUnavailability unavailability =
                unavailabilityRepository.findById(unavailabilityId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Unavailability not found")
                        );

        if (!unavailability.getCounselorId().equals(counselorId)) {
            throw new SecurityException("Not allowed to cancel this unavailability");
        }

        unavailability.setActive(false);
        unavailabilityRepository.save(unavailability);
    }

    /**
     * Calendar view for counselor / UI
     */
    @Override
    public List<CounselorAvailabilityResponse> getAvailabilityForDate(
            Long counselorId,
            LocalDate date
    ) {

        // 1. Get counselor's working hours for the specific day
        Optional<CounselorWorkingHours> workingHoursOpt =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, date.getDayOfWeek());

        if (workingHoursOpt.isEmpty()) {
            return new ArrayList<>(); // Counselor does not work on this day
        }
        CounselorWorkingHours workingHours = workingHoursOpt.get();

        // 2. Get all unavailability blocks for the day
        List<CounselorUnavailability> unavailabilityBlocks =
                unavailabilityRepository.findByCounselorIdAndDateAndActiveTrue(
                        counselorId,
                        date
                );
        Optional<LunchBreak> lunchBreakOpt = lunchBreakRepository.findByCounselorId(counselorId);
        lunchBreakOpt.ifPresent(lunch -> {
            CounselorUnavailability lunchBlock = new CounselorUnavailability();
            lunchBlock.setReason(UnavailabilityReason.LUNCH_BREAK);
            lunchBlock.setStartTime(lunch.getStartTime());
            lunchBlock.setEndTime(lunch.getEndTime());
            unavailabilityBlocks.add(lunchBlock);
        });


        // 3. Generate all possible 1-hour slots
        List<CounselorAvailabilityResponse> allSlots = new ArrayList<>();
        LocalTime slotStart = workingHours.getStartTime();
        while (slotStart.isBefore(workingHours.getEndTime())) {
            LocalTime slotEnd = slotStart.plusHours(1);
            if (!slotEnd.isAfter(workingHours.getEndTime())) {
                allSlots.add(CounselorAvailabilityResponse.builder()
                        .date(date)
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .status("AVAILABLE") // Assume available initially
                        .build());
            }
            slotStart = slotStart.plusHours(1);
        }

        // 4. Determine status for each slot
        return allSlots.stream().map(slot -> {
            for (CounselorUnavailability block : unavailabilityBlocks) {
                // Check for overlap: existing.start < requested.end AND existing.end > requested.start
                if (block.getStartTime().isBefore(slot.getEndTime()) && block.getEndTime().isAfter(slot.getStartTime())) {
                    return CounselorAvailabilityResponse.builder()
                            .date(slot.getDate())
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .status("UNAVAILABLE")
                            .reason(block.getReason().name())
                            .build();
                }
            }
            return slot; // It's still available
        }).collect(Collectors.toList());
    }
}
