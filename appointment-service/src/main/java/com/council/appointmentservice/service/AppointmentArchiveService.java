package com.council.appointmentservice.service;

import com.council.appointmentservice.model.Appointment;
import com.council.appointmentservice.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AppointmentArchiveService {

    private static final int ARCHIVE_AFTER_DAYS = 90;
    private static final int DELETE_AFTER_DAYS = 91; // 1-day overlap
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("MMM-dd-yyyy", Locale.ENGLISH);
    private static final int MAX_RETRIES = 3;
    private static final String LOG_FILE_NAME = "appointment-archive.log";

    private static final Logger logger = LoggerFactory.getLogger(AppointmentArchiveService.class);

    private final AppointmentRepository appointmentRepository;
    private final S3Client r2Client;
    private final String bucket;
    private final String prefix;
    private final boolean enabled;
    private final Path tempDir;

    public AppointmentArchiveService(
            AppointmentRepository appointmentRepository,
            S3Client r2Client,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.prefix:appointments}") String prefix,
            @Value("${appointments.archive.enabled:false}") boolean enabled,
            @Value("${appointments.archive.tmp-dir:./archive}") String tempDir
    ) {
        this.appointmentRepository = appointmentRepository;
        this.r2Client = r2Client;
        this.bucket = bucket;
        this.prefix = prefix;
        this.enabled = enabled;
        this.tempDir = Paths.get(tempDir);
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void archiveAndCleanup() throws IOException {
        if (!enabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate archiveCutoff = today.minusDays(ARCHIVE_AFTER_DAYS);
        LocalDate deleteCutoff = today.minusDays(DELETE_AFTER_DAYS);

        List<Appointment> toArchive = appointmentRepository.findByAppointmentDateBefore(archiveCutoff.plusDays(1));
        boolean archiveSuccess = true;
        if (!toArchive.isEmpty()) {
            archiveSuccess = uploadCsvWithRetry(toArchive);
        }

        if (archiveSuccess) {
            appointmentRepository.deleteByAppointmentDateBefore(deleteCutoff.plusDays(1));
        } else {
            logAdminAlert("Archive failed after retries. Skipping deletion for safety.");
        }
    }

    private boolean uploadCsvWithRetry(List<Appointment> appointments) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                uploadCsv(appointments);
                logInfo("Archive upload succeeded on attempt " + attempt + ".");
                return true;
            } catch (Exception ex) {
                logError("Archive upload failed on attempt " + attempt + ": " + ex.getMessage(), ex);
                if (attempt == MAX_RETRIES) {
                    return false;
                }
            }
        }
        return false;
    }

    private void uploadCsv(List<Appointment> appointments) throws IOException {
        Files.createDirectories(tempDir);
        LocalDate minDate = appointments.stream()
                .map(Appointment::getAppointmentDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());
        LocalDate maxDate = appointments.stream()
                .map(Appointment::getAppointmentDate)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        String fileName = String.format(
                "appointments-%s_to_%s.csv",
                FILE_DATE.format(minDate),
                FILE_DATE.format(maxDate)
        );

        Path tempFile = tempDir.resolve(fileName);
        Files.writeString(tempFile, buildCsv(appointments), StandardCharsets.UTF_8);

        String key = prefix.endsWith("/") ? prefix + fileName : prefix + "/" + fileName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("text/csv")
                .build();

        r2Client.putObject(request, RequestBody.fromFile(tempFile));
        Files.deleteIfExists(tempFile);
    }

    private String buildCsv(List<Appointment> appointments) {
        StringBuilder sb = new StringBuilder();
        sb.append("appointmentId,clientId,counselorId,appointmentDate,startTime,endTime,status,paymentId,createdAt\n");
        for (Appointment a : appointments) {
            sb.append(csv(a.getId()))
                    .append(',').append(csv(a.getClientId()))
                    .append(',').append(csv(a.getCounselorId()))
                    .append(',').append(csv(a.getAppointmentDate()))
                    .append(',').append(csv(a.getStartTime()))
                    .append(',').append(csv(a.getEndTime()))
                    .append(',').append(csv(a.getStatus()))
                    .append(',').append(csv(a.getPaymentId()))
                    .append(',').append(csv(a.getCreatedAt()))
                    .append('\n');
        }
        return sb.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private void logInfo(String message) throws IOException {
        logger.info(message);
        appendLog("INFO", message);
    }

    private void logError(String message, Exception ex) throws IOException {
        logger.error(message, ex);
        appendLog("ERROR", message);
    }

    private void logAdminAlert(String message) throws IOException {
        logger.warn(message);
        appendLog("ALERT", message);
    }

    private void appendLog(String level, String message) throws IOException {
        Files.createDirectories(tempDir);
        String line = String.format("%s [%s] %s%n", LocalDate.now(), level, message);
        Path logFile = tempDir.resolve(LOG_FILE_NAME);
        Files.writeString(logFile, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
