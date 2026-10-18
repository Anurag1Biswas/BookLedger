package com.lms.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PRODUCER responsibility: turn a committed library action into a Kafka message. Nothing else.
 *
 * IssueService raises a {@link BookEvent} inside its database transaction; this listener runs only
 * AFTER that transaction commits, so we never announce an issue/return that was rolled back.
 * A broker outage must not make issuing/returning fail, so errors are logged and swallowed.
 * (Known trade-off: the event is lost if Kafka is down at that moment. A transactional outbox
 * table is the next step if guaranteed delivery is required - see README.)
 */
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BookEventProducer {

//    private static final Logger log = LoggerFactory.getLogger(BookEventProducer.class);
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//    private final String topic;
//
//    public BookEventProducer(KafkaTemplate<String, String> kafkaTemplate,
//                             ObjectMapper objectMapper,
//                             @Value("${app.kafka.topics.book-events}") String topic) {
//        this.kafkaTemplate = kafkaTemplate;
//        this.objectMapper = objectMapper;
//        this.topic = topic;
//    }
//
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
//    public void onBookEvent(BookEvent event) {
//        publish(event);
//    }
//
//    void publish(BookEvent event) {
//        try {
//            String payload = objectMapper.writeValueAsString(event);
//            // key = memberId -> all events of one member stay ordered within a partition
//            kafkaTemplate.send(topic, String.valueOf(event.memberId()), payload)
//                    .whenComplete((result, ex) -> {
//                        if (ex != null) {
//                            log.error("Failed to publish {} (eventId={}) to {}", event.type(), event.eventId(), topic, ex);
//                        } else {
//                            log.info("Published {} (eventId={}) to {}-{}@{}", event.type(), event.eventId(),
//                                    result.getRecordMetadata().topic(),
//                                    result.getRecordMetadata().partition(),
//                                    result.getRecordMetadata().offset());
//                        }
//                    });
//        } catch (JsonProcessingException e) {
//            log.error("Could not serialise {} (eventId={})", event.type(), event.eventId(), e);
//        } catch (RuntimeException e) {
//            // e.g. broker unreachable and metadata fetch timed out (max.block.ms)
//            log.error("Kafka unavailable, {} (eventId={}) was not published", event.type(), event.eventId(), e);
//        }
//    }
}
