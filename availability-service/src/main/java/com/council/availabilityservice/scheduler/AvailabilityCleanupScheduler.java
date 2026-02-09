package com.council.availabilityservice.scheduler;

import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import com.council.availabilityservice.service.PublicHolidaySyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AvailabilityCleanupScheduler {

    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final PublicHolidayRepository holidayRepository;
    private final PublicHolidaySyncService holidaySyncService;

    public AvailabilityCleanupScheduler(
            CounselorUnavailabilityRepository unavailabilityRepository,
            PublicHolidayRepository holidayRepository,
            PublicHolidaySyncService holidaySyncService
    ) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.holidayRepository = holidayRepository;
        this.holidaySyncService = holidaySyncService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyCleanupAndHolidaySync() {
        LocalDate today = LocalDate.now();
        LocalDate keepUntil = today.plusDays(45);

        unavailabilityRepository.deleteAll(
                unavailabilityRepository.findByDateBefore(today)
        );

        holidayRepository.deleteAll(
                holidayRepository.findByHolidayDateBefore(today)
        );

        holidayRepository.deleteAll(
                holidayRepository.findByHolidayDateAfter(keepUntil)
        );

        holidaySyncService.syncHolidaysForNext45Days();
    }
}
