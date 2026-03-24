package com.council.emailservice.service;

import com.council.emailservice.dto.RenderedEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailSenderService implements MailSenderService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSenderService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public SmtpMailSenderService(
            JavaMailSender mailSender,
            @Value("${email.from}") String from,
            @Value("${email.sending.enabled:false}") boolean enabled
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    @Override
    public void send(RenderedEmail email) {
        if (email == null || email.getTo() == null || email.getTo().isBlank()) {
            return;
        }
        if (!enabled) {
            log.info("Email sending disabled. Would send '{}' to {}", email.getSubject(), email.getTo());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email.getTo());
        message.setSubject(email.getSubject());
        message.setText(email.getBody());
        mailSender.send(message);
    }
}
