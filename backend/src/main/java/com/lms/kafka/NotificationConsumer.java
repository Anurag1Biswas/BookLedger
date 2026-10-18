package com.lms.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.exception.InvalidEventException;
import com.lms.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * CONSUMER responsibility: parse the message and hand it to NotificationService. Nothing else.
 * Consumer group: lms-notification-service. Processing is idempotent (unique eventId), so
 * at-least-once delivery from Kafka never produces duplicate notifications.
 * Failures follow the retry / dead-letter policy defined in {@link KafkaConfig}.
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationConsumer {

//    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
//
//    private final ObjectMapper objectMapper;
//    private final NotificationService notificationService;
//
//    public NotificationConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
//        this.objectMapper = objectMapper;
//        this.notificationService = notificationService;
//    }
//
//    @KafkaListener(topics = "${app.kafka.topics.book-events}", groupId = "${app.kafka.consumer-group}")
//    public void onMessage(String payload) {
//        BookEvent event = parse(payload);
//        boolean created = notificationService.recordFromEvent(event);
//        log.info("Consumed {} (eventId={}) -> notification {}", event.type(), event.eventId(),
//                created ? "created" : "already existed (duplicate delivery)");
//    }
//
//    BookEvent parse(String payload) {
//        try {
//            BookEvent event = objectMapper.readValue(payload, BookEvent.class);
//            if (event.eventId() == null || event.type() == null) {
//                throw new InvalidEventException("Event is missing eventId or type", null);
//            }
//            return event;
//        } catch (JsonProcessingException e) {
//            throw new InvalidEventException("Malformed book event", e);
//        }
//    }
}
