package com.council.availabilityservice.service;

import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final PublicHolidayRepository holidayRepository;

    public AvailabilityServiceImpl(
            CounselorUnavailabilityRepository unavailabilityRepository,
            CounselorWorkingHoursRepository workingHoursRepository,
            PublicHolidayRepository holidayRepository
    ) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.holidayRepository = holidayRepository;
    }

    /**
     * INTERNAL
     * Used by Appointment Service before booking
     */
    @Override
    public boolean isSlotAvailable(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {

        /* 1️⃣ Public holiday */
        if (holidayRepository.findByHolidayDate(date).isPresent()) {
            return false;
        }

        /* 2️⃣ Check working hours */
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorId(counselorId)
                        .stream()
                        .filter(w -> w.getDayOfWeek() == dayOfWeek)
                        .findFirst()
                        .orElse(null);

        if (workingHours == null) {
            return false;
        }

        if (startTime.isBefore(workingHours.getStartTime())
                || endTime.isAfter(workingHours.getEndTime())) {
            return false;
        }

        /* 3️⃣ Check overlapping unavailability */
        boolean overlapExists =
                unavailabilityRepository
                        .existsByCounselorIdAndDateAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
                                counselorId,
                                date,
                                endTime,
                                startTime
                        );

        return !overlapExists;
    }

    /**
     * INTERNAL
     * Lock slot for appointment (HOLD / CONFIRMED)
     */
    @Override
    public void blockSlot(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            UnavailabilityReason reason,
            Long referenceId
    ) {

        CounselorUnavailability unavailability = new CounselorUnavailability();
        unavailability.setCounselorId(counselorId);
        unavailability.setDate(date);
        unavailability.setStartTime(startTime);
        unavailability.setEndTime(endTime);
        unavailability.setReason(reason);
        unavailability.setReferenceId(referenceId);

        unavailabilityRepository.save(unavailability);
    }

    /**
     * INTERNAL
     * Free slot when appointment cancelled / expired / rescheduled
     */
    @Override
    public void freeSlot(Long referenceId) {

        List<CounselorUnavailability> records =
                unavailabilityRepository.findByReferenceId(referenceId);

        for (CounselorUnavailability record : records) {
            unavailabilityRepository.delete(record);
        }
    }
}
