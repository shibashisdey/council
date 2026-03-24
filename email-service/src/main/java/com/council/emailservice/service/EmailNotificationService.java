package com.council.emailservice.service;

import com.council.emailservice.client.AuthClient;
import com.council.emailservice.client.CounselorClient;
import com.council.emailservice.client.UserClient;
import com.council.emailservice.dto.AuthUserResponse;
import com.council.emailservice.dto.CounselorResponse;
import com.council.emailservice.dto.EmailNotificationEvent;
import com.council.emailservice.dto.RenderedEmail;
import com.council.emailservice.dto.ResolvedAppointmentContext;
import com.council.emailservice.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final MailSenderService mailSenderService;
    private final EmailTemplateService emailTemplateService;
    private final AuthClient authClient;
    private final UserClient userClient;
    private final CounselorClient counselorClient;

    public EmailNotificationService(
            MailSenderService mailSenderService,
            EmailTemplateService emailTemplateService,
            AuthClient authClient,
            UserClient userClient,
            CounselorClient counselorClient
    ) {
        this.mailSenderService = mailSenderService;
        this.emailTemplateService = emailTemplateService;
        this.authClient = authClient;
        this.userClient = userClient;
        this.counselorClient = counselorClient;
    }

    @KafkaListener(topics = "${email.kafka.topic}")
    public void consume(EmailNotificationEvent event) {
        try {
            ResolvedAppointmentContext context = resolveContext(event);
            List<RenderedEmail> emails = emailTemplateService.render(event, context);
            emails.forEach(mailSenderService::send);
        } catch (RuntimeException e) {
            log.error("Failed to process email event {}", event.getEventType(), e);
        }
    }

    private ResolvedAppointmentContext resolveContext(EmailNotificationEvent event) {
        if ("USER_REGISTERED".equals(event.getEventType())) {
            return ResolvedAppointmentContext.builder().build();
        }

        AuthUserResponse clientAuth = event.getClientUserId() != null
                ? authClient.getUserInternal(event.getClientUserId())
                : null;
        UserProfileResponse userProfile = event.getClientUserId() != null
                ? userClient.getUserPublic(event.getClientUserId())
                : null;
        CounselorResponse counselor = event.getCounselorId() != null
                ? counselorClient.getCounselor(event.getCounselorId())
                : null;
        AuthUserResponse counselorAuth = counselor != null && counselor.getUserId() != null
                ? authClient.getUserInternal(counselor.getUserId())
                : null;

        return ResolvedAppointmentContext.builder()
                .appointmentId(event.getAppointmentId())
                .appointmentDate(event.getAppointmentDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .clientUserId(event.getClientUserId())
                .clientEmail(clientAuth != null ? clientAuth.getEmail() : null)
                .clientName(userProfile != null ? userProfile.getFullName() : null)
                .counselorId(event.getCounselorId())
                .counselorUserId(counselor != null ? counselor.getUserId() : null)
                .counselorEmail(counselorAuth != null ? counselorAuth.getEmail() : null)
                .counselorName(counselor != null ? counselor.getFullName() : null)
                .build();
    }
}
