package com.council.availabilityservice.service;

import com.council.availabilityservice.dto.request.AddUnavailabilityRequest;
import com.council.availabilityservice.dto.request.SafeWorkingHoursUpdateRequest;
import com.council.availabilityservice.dto.request.SetLunchBreakRequest;
import com.council.availabilityservice.dto.request.SetWorkingHoursRequest;
import com.council.availabilityservice.dto.response.CounselorAvailabilityResponse;
import com.council.availabilityservice.dto.response.CounselorScheduleResponse;
import com.council.availabilityservice.dto.response.LunchBreakResponse;
import com.council.availabilityservice.dto.response.SafeWorkingHoursUpdateResponse;
import com.council.availabilityservice.dto.response.UpcomingLeaveResponse;
import com.council.availabilityservice.dto.response.WorkingHoursResponse;
import com.council.availabilityservice.model.CounselorUnavailability;
import com.council.availabilityservice.model.CounselorWorkingHours;
import com.council.availabilityservice.model.LunchBreak;
import com.council.availabilityservice.model.PendingScheduleChange;
import com.council.availabilityservice.model.PendingScheduleChangeStatus;
import com.council.availabilityservice.model.UnavailabilityReason;
import com.council.availabilityservice.repository.CounselorUnavailabilityRepository;
import com.council.availabilityservice.repository.CounselorWorkingHoursRepository;
import com.council.availabilityservice.repository.LunchBreakRepository;
import com.council.availabilityservice.repository.PendingScheduleChangeRepository;
import com.council.availabilityservice.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CounselorScheduleServiceImpl implements CounselorScheduleService {

    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final LunchBreakRepository lunchBreakRepository;
    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final PublicHolidayRepository holidayRepository;
    private final PendingScheduleChangeRepository pendingScheduleChangeRepository;

    public CounselorScheduleServiceImpl(
            CounselorWorkingHoursRepository workingHoursRepository,
            LunchBreakRepository lunchBreakRepository,
            CounselorUnavailabilityRepository unavailabilityRepository,
            PublicHolidayRepository holidayRepository,
            PendingScheduleChangeRepository pendingScheduleChangeRepository
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.lunchBreakRepository = lunchBreakRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.holidayRepository = holidayRepository;
        this.pendingScheduleChangeRepository = pendingScheduleChangeRepository;
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
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Lunch break start and end are required.");
        }
        validateLunchBreakDurationAndOrder(request.getStartTime(), request.getEndTime());

        if (request.getDayOfWeek() != null) {
            upsertLunchBreak(counselorId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime());
            return;
        }

        List<CounselorWorkingHours> workingDays = workingHoursRepository.findByCounselorId(counselorId);
        if (workingDays.isEmpty()) {
            throw new IllegalArgumentException("Set working hours first or provide a day of week for lunch break.");
        }
        for (CounselorWorkingHours workingDay : workingDays) {
            upsertLunchBreak(counselorId, workingDay.getDayOfWeek(), request.getStartTime(), request.getEndTime());
        }
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
    public List<UpcomingLeaveResponse> getUpcomingLeaves(Long counselorId) {
        return unavailabilityRepository
                .findByCounselorIdAndDateGreaterThanEqualAndActiveTrueAndReasonOrderByDateAscStartTimeAsc(
                        counselorId,
                        LocalDate.now(),
                        UnavailabilityReason.COUNSELOR_LEAVE
                )
                .stream()
                .map(leave -> UpcomingLeaveResponse.builder()
                        .id(leave.getId())
                        .date(leave.getDate())
                        .startTime(leave.getStartTime())
                        .endTime(leave.getEndTime())
                        .reason(leave.getReason())
                        .build())
                .toList();
    }

    @Override
    public List<CounselorAvailabilityResponse> getAvailabilityForDate(Long counselorId, LocalDate date) {
        List<CounselorAvailabilityResponse> response = new ArrayList<>();

        ScheduleWindow window = resolveScheduleWindow(counselorId, date);
        if (window == null) {
            return response;
        }

        var holidayOpt = holidayRepository.findByHolidayDate(date);
        String holidayReason = holidayOpt.map(h -> h.getName()).orElse(null);

        List<TimeBlock> blocked = new ArrayList<>();
        if (window.lunchStart != null && window.lunchEnd != null) {
            blocked.add(new TimeBlock(window.lunchStart, window.lunchEnd, "LUNCH_BREAK"));
        }

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

        LocalTime slotStart = window.startTime;
        while (!slotStart.plusHours(1).isAfter(window.endTime)) {
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
    public SafeWorkingHoursUpdateResponse updateWorkingHoursSafely(Long counselorId, SafeWorkingHoursUpdateRequest request) {
        validateSafeUpdateRequest(request);

        DayOfWeek dayOfWeek = request.getDayOfWeek();
        LocalDate today = LocalDate.now();
        List<CounselorUnavailability> appointmentBlocks = unavailabilityRepository
                .findByCounselorIdAndDateGreaterThanEqualAndActiveTrueAndReasonIn(
                        counselorId,
                        today,
                        List.of(UnavailabilityReason.APPOINTMENT_CONFIRMED, UnavailabilityReason.APPOINTMENT_HOLD)
                );

        List<CounselorUnavailability> conflicts = appointmentBlocks.stream()
                .filter(block -> block.getDate().getDayOfWeek() == dayOfWeek)
                .filter(block -> conflictsWithRequestedWindow(block, request))
                .toList();

        cancelPendingChange(counselorId, dayOfWeek);

        if (conflicts.isEmpty()) {
            applyWindowImmediately(counselorId, request);
            return SafeWorkingHoursUpdateResponse.builder()
                    .status("APPLIED_NOW")
                    .message("Working hours updated immediately.")
                    .effectiveFromDate(today)
                    .conflictCount(0)
                    .build();
        }

        LocalDate effectiveFromDate = conflicts.stream()
                .map(CounselorUnavailability::getDate)
                .max(Comparator.naturalOrder())
                .orElse(today)
                .plusDays(1);

        PendingScheduleChange pending = new PendingScheduleChange();
        pending.setCounselorId(counselorId);
        pending.setDayOfWeek(dayOfWeek);
        pending.setStartTime(request.getStartTime());
        pending.setEndTime(request.getEndTime());
        pending.setLunchStartTime(request.getLunchStartTime());
        pending.setLunchEndTime(request.getLunchEndTime());
        pending.setEffectiveFromDate(effectiveFromDate);
        pending.setStatus(PendingScheduleChangeStatus.PENDING);
        pendingScheduleChangeRepository.save(pending);

        return SafeWorkingHoursUpdateResponse.builder()
                .status("SCHEDULED_FOR")
                .message("Schedule change is queued and will apply after existing appointments complete.")
                .effectiveFromDate(effectiveFromDate)
                .conflictCount(conflicts.size())
                .build();
    }

    @Override
    public SafeWorkingHoursUpdateResponse removeWorkingDaySafely(Long counselorId, DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        List<CounselorUnavailability> appointmentBlocks = unavailabilityRepository
                .findByCounselorIdAndDateGreaterThanEqualAndActiveTrueAndReasonIn(
                        counselorId,
                        today,
                        List.of(UnavailabilityReason.APPOINTMENT_CONFIRMED, UnavailabilityReason.APPOINTMENT_HOLD)
                );

        List<CounselorUnavailability> conflicts = appointmentBlocks.stream()
                .filter(block -> block.getDate().getDayOfWeek() == dayOfWeek)
                .toList();

        cancelPendingChange(counselorId, dayOfWeek);

        if (conflicts.isEmpty()) {
            workingHoursRepository.deleteByCounselorIdAndDayOfWeek(counselorId, dayOfWeek);
            lunchBreakRepository.deleteByCounselorIdAndDayOfWeek(counselorId, dayOfWeek);
            return SafeWorkingHoursUpdateResponse.builder()
                    .status("APPLIED_NOW")
                    .message("Working day removed immediately.")
                    .effectiveFromDate(today)
                    .conflictCount(0)
                    .build();
        }

        LocalDate effectiveFromDate = conflicts.stream()
                .map(CounselorUnavailability::getDate)
                .max(Comparator.naturalOrder())
                .orElse(today)
                .plusDays(1);

        PendingScheduleChange pending = new PendingScheduleChange();
        pending.setCounselorId(counselorId);
        pending.setDayOfWeek(dayOfWeek);
        pending.setOffDay(true);
        pending.setEffectiveFromDate(effectiveFromDate);
        pending.setStatus(PendingScheduleChangeStatus.PENDING);
        pendingScheduleChangeRepository.save(pending);

        return SafeWorkingHoursUpdateResponse.builder()
                .status("SCHEDULED_FOR")
                .message("Working day removal is queued and will apply after existing appointments complete.")
                .effectiveFromDate(effectiveFromDate)
                .conflictCount(conflicts.size())
                .build();
    }

    @Override
    public CounselorScheduleResponse getSchedule(Long counselorId) {
        List<CounselorWorkingHours> workingHourEntities = workingHoursRepository.findByCounselorId(counselorId);

        List<WorkingHoursResponse> workingHours = workingHourEntities
                .stream()
                .map(hours -> WorkingHoursResponse.builder()
                        .dayOfWeek(hours.getDayOfWeek())
                        .startTime(hours.getStartTime())
                        .endTime(hours.getEndTime())
                        .build())
                .collect(Collectors.toList());

        List<LunchBreak> lunchBreakEntities = lunchBreakRepository.findByCounselorId(counselorId);
        Map<DayOfWeek, LunchBreak> lunchByDay = lunchBreakEntities.stream()
                .filter(entry -> entry.getDayOfWeek() != null)
                .collect(Collectors.toMap(LunchBreak::getDayOfWeek, Function.identity(), (a, b) -> b));
        LunchBreak legacyLunch = lunchBreakEntities.stream()
                .filter(entry -> entry.getDayOfWeek() == null)
                .findFirst()
                .orElse(null);

        List<LunchBreakResponse> lunchBreaks = workingHourEntities.stream()
                .map(workingHoursEntity -> {
                    LunchBreak lunch = lunchByDay.get(workingHoursEntity.getDayOfWeek());
                    if (lunch == null) {
                        lunch = legacyLunch;
                    }
                    if (lunch == null) {
                        return null;
                    }
                    return LunchBreakResponse.builder()
                            .dayOfWeek(workingHoursEntity.getDayOfWeek())
                            .startTime(lunch.getStartTime())
                            .endTime(lunch.getEndTime())
                            .build();
                })
                .filter(entry -> entry != null)
                .toList();

        LunchBreakResponse lunchBreak = lunchBreaks.isEmpty()
                ? null
                : lunchBreaks.get(0);

        return CounselorScheduleResponse.builder()
                .workingHours(workingHours)
                .lunchBreaks(lunchBreaks)
                .lunchBreak(lunchBreak)
                .build();
    }

    private void validateSafeUpdateRequest(SafeWorkingHoursUpdateRequest request) {
        if (request.getDayOfWeek() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Day and work hours are required.");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        long workMinutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (workMinutes > 8 * 60) {
            throw new IllegalArgumentException("Working hours cannot exceed 8 hours.");
        }
        boolean lunchStartProvided = request.getLunchStartTime() != null;
        boolean lunchEndProvided = request.getLunchEndTime() != null;
        if (lunchStartProvided != lunchEndProvided) {
            throw new IllegalArgumentException("Lunch break start and end must both be provided or both be empty.");
        }
        if (!lunchStartProvided) {
            return;
        }
        if (!request.getLunchEndTime().equals(request.getLunchStartTime().plusHours(1))) {
            throw new IllegalArgumentException("Lunch break must be exactly 1 hour.");
        }
        boolean lunchInside = !request.getLunchStartTime().isBefore(request.getStartTime())
                && !request.getLunchEndTime().isAfter(request.getEndTime());
        if (!lunchInside) {
            throw new IllegalArgumentException("Lunch break must be inside working hours.");
        }
    }

    private boolean conflictsWithRequestedWindow(CounselorUnavailability block, SafeWorkingHoursUpdateRequest request) {
        LocalTime lunchStart = request.getLunchStartTime();
        LocalTime lunchEnd = request.getLunchEndTime();
        boolean outsideWork = block.getStartTime().isBefore(request.getStartTime())
                || block.getEndTime().isAfter(request.getEndTime());
        boolean overlapsLunch = lunchStart != null
                && lunchEnd != null
                && block.getStartTime().isBefore(lunchEnd)
                && block.getEndTime().isAfter(lunchStart);
        return outsideWork || overlapsLunch;
    }

    private void applyWindowImmediately(Long counselorId, SafeWorkingHoursUpdateRequest request) {
        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, request.getDayOfWeek())
                        .orElse(new CounselorWorkingHours());
        workingHours.setCounselorId(counselorId);
        workingHours.setDayOfWeek(request.getDayOfWeek());
        workingHours.setStartTime(request.getStartTime());
        workingHours.setEndTime(request.getEndTime());
        workingHoursRepository.save(workingHours);

        if (request.getLunchStartTime() == null || request.getLunchEndTime() == null) {
            lunchBreakRepository.deleteByCounselorIdAndDayOfWeek(counselorId, request.getDayOfWeek());
            return;
        }
        upsertLunchBreak(counselorId, request.getDayOfWeek(), request.getLunchStartTime(), request.getLunchEndTime());
    }

    private void cancelPendingChange(Long counselorId, DayOfWeek dayOfWeek) {
        pendingScheduleChangeRepository
                .findTopByCounselorIdAndDayOfWeekAndStatusOrderByCreatedAtDesc(
                        counselorId,
                        dayOfWeek,
                        PendingScheduleChangeStatus.PENDING
                )
                .ifPresent(existing -> {
                    existing.setStatus(PendingScheduleChangeStatus.CANCELLED);
                    pendingScheduleChangeRepository.save(existing);
                });
    }

    private ScheduleWindow resolveScheduleWindow(Long counselorId, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        var pendingOpt = pendingScheduleChangeRepository
                .findTopByCounselorIdAndDayOfWeekAndStatusOrderByCreatedAtDesc(
                        counselorId,
                        dayOfWeek,
                        PendingScheduleChangeStatus.PENDING
                )
                .filter(pending -> !date.isBefore(pending.getEffectiveFromDate()));

        if (pendingOpt.isPresent()) {
            PendingScheduleChange pending = pendingOpt.get();
            if (pending.isOffDay()) {
                return null;
            }
            return new ScheduleWindow(
                    pending.getStartTime(),
                    pending.getEndTime(),
                    pending.getLunchStartTime(),
                    pending.getLunchEndTime()
            );
        }

        CounselorWorkingHours workingHours =
                workingHoursRepository.findByCounselorIdAndDayOfWeek(counselorId, dayOfWeek)
                        .orElse(null);
        if (workingHours == null) {
            return null;
        }
        LunchBreak lunch = resolveLunchBreakForDay(counselorId, dayOfWeek);
        return new ScheduleWindow(
                workingHours.getStartTime(),
                workingHours.getEndTime(),
                lunch != null ? lunch.getStartTime() : null,
                lunch != null ? lunch.getEndTime() : null
        );
    }

    private void validateLunchBreakDurationAndOrder(LocalTime startTime, LocalTime endTime) {
        if (!endTime.equals(startTime.plusHours(1))) {
            throw new IllegalArgumentException("Lunch break must be exactly 1 hour.");
        }
    }

    private void upsertLunchBreak(Long counselorId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        LunchBreak lunchBreak = lunchBreakRepository.findByCounselorIdAndDayOfWeek(counselorId, dayOfWeek)
                .orElse(new LunchBreak());
        lunchBreak.setCounselorId(counselorId);
        lunchBreak.setDayOfWeek(dayOfWeek);
        lunchBreak.setStartTime(startTime);
        lunchBreak.setEndTime(endTime);
        lunchBreakRepository.save(lunchBreak);
    }

    private LunchBreak resolveLunchBreakForDay(Long counselorId, DayOfWeek dayOfWeek) {
        return lunchBreakRepository.findByCounselorIdAndDayOfWeek(counselorId, dayOfWeek)
                .or(() -> lunchBreakRepository.findTopByCounselorIdAndDayOfWeekIsNull(counselorId))
                .orElse(null);
    }

    private static class ScheduleWindow {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final LocalTime lunchStart;
        private final LocalTime lunchEnd;

        private ScheduleWindow(LocalTime startTime, LocalTime endTime, LocalTime lunchStart, LocalTime lunchEnd) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.lunchStart = lunchStart;
            this.lunchEnd = lunchEnd;
        }
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
