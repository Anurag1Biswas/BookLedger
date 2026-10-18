package com.lms.kafka;

import com.lms.exception.InvalidEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Topics + consumer error handling.
 *
 * Retry policy: a failing record is retried 3 more times, 1 second apart. If it still fails
 * (or is a malformed message, which is never retried) it is published to "book-events.DLT"
 * so one bad message can never block the partition.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

//    @Bean
//    public NewTopic bookEventsTopic(@Value("${app.kafka.topics.book-events}") String name,
//                                    @Value("${app.kafka.topics.partitions:3}") int partitions,
//                                    @Value("${app.kafka.topics.replicas:1}") int replicas) {
//        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build();
//    }
//
//    /** Dead-letter topic: same partition count because the recoverer keeps the original partition number. */
//    @Bean
//    public NewTopic bookEventsDeadLetterTopic(@Value("${app.kafka.topics.book-events}") String name,
//                                              @Value("${app.kafka.topics.partitions:3}") int partitions,
//                                              @Value("${app.kafka.topics.replicas:1}") int replicas) {
//        return TopicBuilder.name(name + ".DLT").partitions(partitions).replicas(replicas).build();
//    }
//
//    @Bean
//    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
//        DefaultErrorHandler handler = new DefaultErrorHandler(
//                new DeadLetterPublishingRecoverer(template),
//                new FixedBackOff(1000L, 3L));
//        handler.addNotRetryableExceptions(InvalidEventException.class);
//        return handler;
//    }
}
