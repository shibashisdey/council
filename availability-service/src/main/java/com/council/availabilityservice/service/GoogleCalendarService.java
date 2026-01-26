package com.council.availabilityservice.service;

import com.council.availabilityservice.config.GoogleCalendarConfig;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class GoogleCalendarService {

    private final GoogleCalendarConfig calendarConfig;

    public GoogleCalendarService(GoogleCalendarConfig calendarConfig) {
        this.calendarConfig = calendarConfig;
    }

    /**
     * Create a blocking event in counselor's Google Calendar
     *
     * @param counselorId The counselor's user ID
     * @param eventTitle Title of the event
     * @param date Date of the event
     * @param startTime Start time
     * @param endTime End time
     * @param description Event description
     * @return Event ID from Google Calendar
     */
    public String createBlockingEvent(
            Long counselorId,
            String eventTitle,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String description
    ) throws GeneralSecurityException, IOException {

        Calendar service = calendarConfig.getCalendarService(counselorId.toString());

        // Create event object
        Event event = new Event()
                .setSummary(eventTitle)
                .setDescription(description);

        // Set start time
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        DateTime start = new DateTime(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        event.setStart(new EventDateTime().setDateTime(start).setTimeZone("Asia/Kolkata"));

        // Set end time
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        DateTime end = new DateTime(
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        event.setEnd(new EventDateTime().setDateTime(end).setTimeZone("Asia/Kolkata"));

        // Mark as busy (blocks the time slot)
        event.setTransparency("opaque");

        // Insert event into primary calendar
        event = service.events().insert("primary", event).execute();

        System.out.println("Event created: " + event.getHtmlLink());
        return event.getId();
    }

    /**
     * Delete an event from counselor's Google Calendar
     *
     * @param counselorId The counselor's user ID
     * @param eventId The Google Calendar event ID
     */
    public void deleteEvent(Long counselorId, String eventId)
            throws GeneralSecurityException, IOException {

        Calendar service = calendarConfig.getCalendarService(counselorId.toString());
        service.events().delete("primary", eventId).execute();

        System.out.println("Event deleted: " + eventId);
    }

    /**
     * Get all events for a counselor on a specific date
     *
     * @param counselorId The counselor's user ID
     * @param date The date to check
     * @return List of events
     */
    public List<Event> getEventsForDate(Long counselorId, LocalDate date)
            throws GeneralSecurityException, IOException {

        Calendar service = calendarConfig.getCalendarService(counselorId.toString());

        // Set time range for the day
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        DateTime timeMin = new DateTime(
                Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant())
        );
        DateTime timeMax = new DateTime(
                Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant())
        );

        // Query events
        Events events = service.events().list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems() != null ? events.getItems() : new ArrayList<>();
    }

    /**
     * Check if a time slot is free in counselor's Google Calendar
     *
     * @param counselorId The counselor's user ID
     * @param date Date to check
     * @param startTime Start time of slot
     * @param endTime End time of slot
     * @return true if slot is free, false if busy
     */
    public boolean isSlotFreeInCalendar(
            Long counselorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) throws GeneralSecurityException, IOException {

        List<Event> events = getEventsForDate(counselorId, date);

        LocalDateTime slotStart = LocalDateTime.of(date, startTime);
        LocalDateTime slotEnd = LocalDateTime.of(date, endTime);

        for (Event event : events) {
            // Skip all-day events or events without specific times
            if (event.getStart().getDateTime() == null) {
                continue;
            }

            LocalDateTime eventStart = LocalDateTime.ofInstant(
                    new Date(event.getStart().getDateTime().getValue()).toInstant(),
                    ZoneId.systemDefault()
            );

            LocalDateTime eventEnd = LocalDateTime.ofInstant(
                    new Date(event.getEnd().getDateTime().getValue()).toInstant(),
                    ZoneId.systemDefault()
            );

            // Check for overlap
            if (!(slotEnd.isBefore(eventStart) || slotStart.isAfter(eventEnd))) {
                return false; // Slot overlaps with existing event
            }
        }

        return true; // Slot is free
    }

    /**
     * Sync working hours to Google Calendar (creates recurring event)
     *
     * @param counselorId The counselor's user ID
     * @param dayOfWeek Day of week (e.g., "MONDAY")
     * @param startTime Working hours start
     * @param endTime Working hours end
     */
    public String syncWorkingHours(
            Long counselorId,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) throws GeneralSecurityException, IOException {

        Calendar service = calendarConfig.getCalendarService(counselorId.toString());

        Event event = new Event()
                .setSummary("Working Hours - " + dayOfWeek)
                .setDescription("Available for counseling sessions");

        // Set recurrence rule (every week on this day)
        String recurrence = "RRULE:FREQ=WEEKLY;BYDAY=" + getDayAbbreviation(dayOfWeek);
        event.setRecurrence(List.of(recurrence));

        // Set start time (use next occurrence of this day of week)
        LocalDate nextDate = getNextDayOfWeek(dayOfWeek);
        LocalDateTime startDateTime = LocalDateTime.of(nextDate, startTime);
        DateTime start = new DateTime(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        event.setStart(new EventDateTime().setDateTime(start).setTimeZone("Asia/Kolkata"));

        // Set end time
        LocalDateTime endDateTime = LocalDateTime.of(nextDate, endTime);
        DateTime end = new DateTime(
                Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
        event.setEnd(new EventDateTime().setDateTime(end).setTimeZone("Asia/Kolkata"));

        // Mark as free (not blocking)
        event.setTransparency("transparent");

        event = service.events().insert("primary", event).execute();
        return event.getId();
    }

    // Helper methods
    private String getDayAbbreviation(String dayOfWeek) {
        return switch (dayOfWeek.toUpperCase()) {
            case "MONDAY" -> "MO";
            case "TUESDAY" -> "TU";
            case "WEDNESDAY" -> "WE";
            case "THURSDAY" -> "TH";
            case "FRIDAY" -> "FR";
            case "SATURDAY" -> "SA";
            case "SUNDAY" -> "SU";
            default -> throw new IllegalArgumentException("Invalid day: " + dayOfWeek);
        };
    }

    private LocalDate getNextDayOfWeek(String dayOfWeek) {
        LocalDate today = LocalDate.now();
        java.time.DayOfWeek target = java.time.DayOfWeek.valueOf(dayOfWeek.toUpperCase());

        LocalDate nextOccurrence = today;
        while (nextOccurrence.getDayOfWeek() != target) {
            nextOccurrence = nextOccurrence.plusDays(1);
        }

        return nextOccurrence;
    }
}