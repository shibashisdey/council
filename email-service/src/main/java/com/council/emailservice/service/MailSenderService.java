package com.council.emailservice.service;

import com.council.emailservice.dto.RenderedEmail;

public interface MailSenderService {
    void send(RenderedEmail email);
}
