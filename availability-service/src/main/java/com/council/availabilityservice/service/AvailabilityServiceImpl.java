package com.council.availabilityservice.service;

import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.model.PublicHoliday;
import com.council.availabilityservice.repository.LunchBreakRepository;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final PublicHolidayRepository holidayRepository;
    private final LunchBreakRepository lunchBreakRepository;

    public AvailabilityServiceImpl(
            CounselorUnavailabilityRepository unavailabilityRepository,
            CounselorWorkingHoursRepository workingHoursRepository,
            PublicHolidayRepository holidayRepository,
            LunchBreakRepository lunchBreakRepository
    ) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.holidayRepository = holidayRepository;
        this.lunchBreakRepository = lunchBreakRepository;
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
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, dayOfWeek)
                        .orElse(null);

        if (workingHours == null) {
            return false; // Not working on this day
        }

        if (startTime.isBefore(workingHours.getStartTime()) || endTime.isAfter(workingHours.getEndTime())) {
            return false; // Outside working hours
        }

        /* 3️⃣ Check for Lunch Break Overlap */
        Optional<LunchBreak> lunchBreakOpt = lunchBreakRepository.findByCounselorId(counselorId);
        if (lunchBreakOpt.isPresent()) {
            LunchBreak lunch = lunchBreakOpt.get();
            // Overlap check: existing.start < requested.end AND existing.end > requested.start
            if (lunch.getStartTime().isBefore(endTime) && lunch.getEndTime().isAfter(startTime)) {
                return false;
            }
        }

        /* 4️⃣ Check for other overlapping unavailability (Leaves, Appointments) */
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

        /* 5️⃣ Check for 7-hour daily limit */
        long confirmedAppointments = unavailabilityRepository
                .countByCounselorIdAndDateAndActiveTrueAndReason(
                        counselorId,
                        date,
                        UnavailabilityReason.APPOINTMENT_CONFIRMED
                );

        if (confirmedAppointments >= 7) {
            return false;
        }

        return true; // If all checks pass, the slot is available
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

        // IDEMPOTENCY CHECK: Do not create a duplicate block if one already exists.
        if (unavailabilityRepository.existsByReferenceIdAndActiveTrue(referenceId)) {
            return; // Already blocked, do nothing.
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

    /**
     * INTERNAL
     * Free slot when appointment cancelled / expired / rescheduled
     */
    @Override
    public void freeSlot(Long referenceId) {

        List<CounselorUnavailability> records =
                unavailabilityRepository.findByReferenceId(referenceId);

        for (CounselorUnavailability record : records) {
            record.setActive(false);
            unavailabilityRepository.save(record);
        }
    }

    /**
     * INTERNAL
     * Update the reason for an existing block. Idempotent.
     */
    @Override
    public void updateBlockReason(Long referenceId, UnavailabilityReason newReason) {
        unavailabilityRepository.findTopByReferenceIdAndActiveTrueOrderByDateDesc(referenceId)
                .ifPresent(unavailability -> {
                    unavailability.setReason(newReason);
                    unavailabilityRepository.save(unavailability);
                });
    }

    // ===========================================
    // Schedulers for data retention and holidays
    // ===========================================

    /**
     * Daily scheduler:
     * - Delete unavailability records older than today
     * - Delete holiday records older than today
     * - Refresh public holidays from Google Calendar (mock for now)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    public void dailyCleanupAndHolidayRefresh() {
        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = today.minusDays(1); // Records older than yesterday

        // Delete unavailability records older than today
        // Note: soft deleted records are not explicitly deleted here, only truly inactive ones.
        // For hard deletion, need to query by active=false and date < today.
        // For now, only hard delete truly old records, soft delete for cancellation/free.
        unavailabilityRepository.deleteAll(
                unavailabilityRepository.findByDateBefore(cutoffDate)
        );

        // Delete holiday records older than today
        holidayRepository.deleteAll(
                holidayRepository.findByHolidayDateBefore(cutoffDate)
        );

        // Refresh public holidays (mock for now)
        refreshPublicHolidays();
    }

    private void refreshPublicHolidays() {
        // This is a mock implementation.
        // In a real scenario, this would call Google Calendar API.
        // For now, it adds a few fixed holidays for the next 45 days.

        LocalDate today = LocalDate.now();
        LocalDate fortyFiveDaysFromNow = today.plusDays(45);

        // Clear existing holidays within the window to ensure we only store 45 days ahead
        holidayRepository.deleteAll(
                holidayRepository.findByHolidayDateGreaterThanEqualAndHolidayDateLessThanEqual(
                        today, fortyFiveDaysFromNow
                )
        );

        // Example mock holidays (replace with actual API calls)
        addMockHoliday(today.plusDays(7), "Mock Holiday 1", "IN");
        addMockHoliday(today.plusDays(15), "Mock Holiday 2", "IN");
        addMockHoliday(today.plusDays(30), "Mock Holiday 3", "IN");
    }

    private void addMockHoliday(LocalDate date, String name, String countryCode) {
        if (holidayRepository.findByHolidayDateAndCountryCode(date, countryCode).isEmpty()) {
            PublicHoliday holiday = new PublicHoliday();
            holiday.setHolidayDate(date);
            holiday.setName(name);
            holiday.setCountryCode(countryCode);
            holidayRepository.save(holiday);
        }
    }
}
