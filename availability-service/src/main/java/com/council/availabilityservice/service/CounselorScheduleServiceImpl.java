package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CounselorScheduleServiceImpl implements CounselorScheduleService {

    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final CounselorUnavailabilityRepository unavailabilityRepository;

    public CounselorScheduleServiceImpl(
            CounselorWorkingHoursRepository workingHoursRepository,
            CounselorUnavailabilityRepository unavailabilityRepository
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.unavailabilityRepository = unavailabilityRepository;
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
                workingHoursRepository.findByCounselorId(counselorId)
                        .stream()
                        .filter(w -> w.getDayOfWeek() == request.getDayOfWeek())
                        .findFirst()
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

        unavailabilityRepository.delete(unavailability);
    }

    /**
     * Calendar view for counselor / UI
     */
    @Override
    public List<CounselorAvailabilityResponse> getAvailabilityForDate(
            Long counselorId,
            LocalDate date
    ) {

        List<CounselorAvailabilityResponse> response = new ArrayList<>();

        List<CounselorUnavailability> blocks =
                unavailabilityRepository.findByCounselorIdAndDateAndActiveTrue(
                        counselorId,
                        date
                );

        for (CounselorUnavailability block : blocks) {
            response.add(
                    CounselorAvailabilityResponse.builder()
                            .date(date)
                            .startTime(block.getStartTime())
                            .endTime(block.getEndTime())
                            .status("UNAVAILABLE")
                            .reason(block.getReason().name())
                            .build()
            );
        }

        return response;
    }
}
