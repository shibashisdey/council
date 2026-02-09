package com.council.availabilityservice.service;

import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.LunchBreakRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final LunchBreakRepository lunchBreakRepository;
    private final PublicHolidayRepository holidayRepository;

    public AvailabilityServiceImpl(
            CounselorUnavailabilityRepository unavailabilityRepository,
            CounselorWorkingHoursRepository workingHoursRepository,
            LunchBreakRepository lunchBreakRepository,
            PublicHolidayRepository holidayRepository
    ) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.lunchBreakRepository = lunchBreakRepository;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public boolean isSlotAvailable(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (holidayRepository.findByHolidayDate(date).isPresent()) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, dayOfWeek)
                        .orElse(null);
        if (workingHours == null) {
            return false;
        }

        if (startTime.isBefore(workingHours.getStartTime())
                || endTime.isAfter(workingHours.getEndTime())) {
            return false;
        }

        Optional<LunchBreak> lunchBreakOpt = lunchBreakRepository.findByCounselorId(counselorId);
        if (lunchBreakOpt.isPresent()) {
            LunchBreak lunch = lunchBreakOpt.get();
            if (lunch.getStartTime().isBefore(endTime)
                    && lunch.getEndTime().isAfter(startTime)) {
                return false;
            }
        }

        boolean overlapExists =
                unavailabilityRepository
                        .existsByCounselorIdAndDateAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
                                counselorId,
                                date,
                                endTime,
                                startTime
                        );
        if (overlapExists) {
            return false;
        }

        long confirmedAppointments = unavailabilityRepository
                .countByCounselorIdAndDateAndActiveTrueAndReason(
                        counselorId,
                        date,
                        UnavailabilityReason.APPOINTMENT_CONFIRMED
                );

        return confirmedAppointments < 7;
    }

    @Override
    public void blockSlot(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UnavailabilityReason reason,
            Long referenceId
    ) {
        if (referenceId != null && unavailabilityRepository.existsByReferenceIdAndActiveTrue(referenceId)) {
            return;
        }

        CounselorUnavailability unavailability = new CounselorUnavailability();
        unavailability.setCounselorId(counselorId);
        unavailability.setDate(date);
        unavailability.setStartTime(startTime);
        unavailability.setEndTime(endTime);
        unavailability.setReason(reason);
        unavailability.setReferenceId(referenceId);

        unavailabilityRepository.save(unavailability);
    }

    @Override
    public void updateBlockReason(Long referenceId, UnavailabilityReason newReason) {
        if (referenceId == null || newReason == null) {
            return;
        }

        unavailabilityRepository.findTopByReferenceIdAndActiveTrueOrderByDateDesc(referenceId)
                .ifPresent(unavailability -> {
                    unavailability.setReason(newReason);
                    unavailabilityRepository.save(unavailability);
                });
    }

    @Override
    public void freeSlot(Long referenceId) {
        if (referenceId == null) {
            return;
        }

        List<CounselorUnavailability> records =
                unavailabilityRepository.findByReferenceId(referenceId);
        for (CounselorUnavailability record : records) {
            record.setActive(false);
            unavailabilityRepository.save(record);
        }
    }
}
