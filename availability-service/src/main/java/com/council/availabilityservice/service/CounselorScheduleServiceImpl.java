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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CounselorScheduleServiceImpl implements CounselorScheduleService {

    private final CounselorWorkingHoursRepository workingHoursRepository;
    private final CounselorUnavailabilityRepository unavailabilityRepository;
    private final GoogleCalendarService googleCalendarService;

    public CounselorScheduleServiceImpl(
            CounselorWorkingHoursRepository workingHoursRepository,
            CounselorUnavailabilityRepository unavailabilityRepository,
            GoogleCalendarService googleCalendarService
    ) {
        this.workingHoursRepository = workingHoursRepository;
        this.unavailabilityRepository = unavailabilityRepository;
        this.googleCalendarService = googleCalendarService;
    }

    /**
     * Counselor sets weekly working hours
     * Also syncs to Google Calendar
     */
    @Override
    public void setWorkingHours(
            Long counselorId,
            SetWorkingHoursRequest request
    ) {

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

        // Sync to Google Calendar (non-blocking)
        try {
            googleCalendarService.syncWorkingHours(
                    counselorId,
                    request.getDayOfWeek().name(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            System.out.println("✓ Working hours synced to Google Calendar");
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("⚠ Failed to sync to Google Calendar: " + e.getMessage());
            // Continue execution - don't fail if Google Calendar sync fails
        }
    }

    /**
     * Counselor marks unavailability
     * Also blocks time in Google Calendar
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

        // Create blocking event in Google Calendar
        try {
            String eventTitle = getEventTitle(request.getReason());
            String eventId = googleCalendarService.createBlockingEvent(
                    counselorId,
                    eventTitle,
                    request.getDate(),
                    request.getStartTime(),
                    request.getEndTime(),
                    "Blocked via Council Therapy App - " + request.getReason()
            );

            // Store Google Calendar event ID for future reference
            unavailability.setReferenceId(Long.valueOf(eventId.hashCode()));
            unavailabilityRepository.save(unavailability);

            System.out.println("✓ Unavailability synced to Google Calendar");
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("⚠ Failed to sync to Google Calendar: " + e.getMessage());
            // Continue execution - event is still saved locally
        }
    }

    /**
     * Counselor cancels previously added unavailability
     * Also removes from Google Calendar
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

        // Delete from database
        unavailabilityRepository.delete(unavailability);

        // Delete from Google Calendar (if referenceId exists)
        if (unavailability.getReferenceId() != null) {
            try {
                // Note: You'll need to store actual Google Calendar event ID
                // For now, this is a placeholder
                System.out.println("⚠ Google Calendar event deletion not fully implemented");
                // googleCalendarService.deleteEvent(counselorId, eventId);
            } catch (Exception e) {
                System.err.println("⚠ Failed to delete from Google Calendar: " + e.getMessage());
            }
        }
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

    // Helper method to generate event titles
    private String getEventTitle(String reason) {
        return switch (reason) {
            case "LUNCH_BREAK" -> "🍽️ Lunch Break";
            case "COUNSELOR_LEAVE" -> "🏖️ Out of Office";
            case "PUBLIC_HOLIDAY" -> "🎉 Public Holiday";
            case "APPOINTMENT_HOLD" -> "⏳ Appointment (Pending Payment)";
            case "APPOINTMENT_CONFIRMED" -> "📅 Counseling Session";
            default -> "⛔ Blocked";
        };
    }
}