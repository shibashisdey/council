package com.council.notificationservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailEventPublisher.class);

    private final KafkaTemplate<String, EmailNotificationEvent> kafkaTemplate;
    private final String topic;

    public EmailEventPublisher(
            KafkaTemplate<String, EmailNotificationEvent> kafkaTemplate,
            @Value("${email.kafka.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(EmailNotificationEvent event) {
        try {
            kafkaTemplate.send(topic, event.getEventType(), event);
        } catch (RuntimeException e) {
            log.warn("Failed to publish email event {}", event.getEventType(), e);
        }
    }
}
