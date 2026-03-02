package com.council.availabilityservice.scheduler;

import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.model.PendingScheduleChange;
import com.council.availabilityservice.model.PendingScheduleChangeStatus;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.LunchBreakRepository;
import com.council.availabilityservice.repository.PendingScheduleChangeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class PendingScheduleChangeScheduler {

    private final PendingScheduleChangeRepository pendingScheduleChangeRepository;
    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final LunchBreakRepository lunchBreakRepository;

    public PendingScheduleChangeScheduler(
            PendingScheduleChangeRepository pendingScheduleChangeRepository,
            CounselorWorkingHoursRepository workingHoursRepository,
            LunchBreakRepository lunchBreakRepository
    ) {
        this.pendingScheduleChangeRepository = pendingScheduleChangeRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.lunchBreakRepository = lunchBreakRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void applyDueScheduleChanges() {
        List<PendingScheduleChange> dueChanges =
                pendingScheduleChangeRepository.findByStatusAndEffectiveFromDateLessThanEqual(
                        PendingScheduleChangeStatus.PENDING,
                        LocalDate.now()
                );

        for (PendingScheduleChange pending : dueChanges) {
            if (pending.isOffDay()) {
                workingHoursRepository.deleteByCounselorIdAndDayOfWeek(
                        pending.getCounselorId(),
                        pending.getDayOfWeek()
                );
                lunchBreakRepository.deleteByCounselorIdAndDayOfWeek(
                        pending.getCounselorId(),
                        pending.getDayOfWeek()
                );
                pending.setStatus(PendingScheduleChangeStatus.APPLIED);
                pendingScheduleChangeRepository.save(pending);
                continue;
            }

            CounselorWorkingHours workingHours =
                    workingHoursRepository.findByCounselorIdAndDayOfWeek(
                                    pending.getCounselorId(),
                                    pending.getDayOfWeek()
                            )
                            .orElse(new CounselorWorkingHours());
            workingHours.setCounselorId(pending.getCounselorId());
            workingHours.setDayOfWeek(pending.getDayOfWeek());
            workingHours.setStartTime(pending.getStartTime());
            workingHours.setEndTime(pending.getEndTime());
            workingHoursRepository.save(workingHours);

            if (pending.getLunchStartTime() == null || pending.getLunchEndTime() == null) {
                lunchBreakRepository.deleteByCounselorIdAndDayOfWeek(
                        pending.getCounselorId(),
                        pending.getDayOfWeek()
                );
            } else {
                LunchBreak lunchBreak = lunchBreakRepository.findByCounselorIdAndDayOfWeek(
                                pending.getCounselorId(),
                                pending.getDayOfWeek()
                        )
                        .orElse(new LunchBreak());
                lunchBreak.setCounselorId(pending.getCounselorId());
                lunchBreak.setDayOfWeek(pending.getDayOfWeek());
                lunchBreak.setStartTime(pending.getLunchStartTime());
                lunchBreak.setEndTime(pending.getLunchEndTime());
                lunchBreakRepository.save(lunchBreak);
            }

            pending.setStatus(PendingScheduleChangeStatus.APPLIED);
            pendingScheduleChangeRepository.save(pending);
        }
    }
}
