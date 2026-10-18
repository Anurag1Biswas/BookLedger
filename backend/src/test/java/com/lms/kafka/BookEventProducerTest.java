package com.lms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookEventProducerTest {

//    private static final String TOPIC = "book-events";
//
//    @Mock KafkaTemplate<String, String> kafkaTemplate;
//
//    private final ObjectMapper mapper = new ObjectMapper()
//            .registerModule(new JavaTimeModule())
//            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//    private BookEventProducer producer;
//
//    @BeforeEach
//    void setUp() {
//        producer = new BookEventProducer(kafkaTemplate, mapper, TOPIC);
//    }
//
//    @SuppressWarnings("unchecked")
//    private CompletableFuture<SendResult<String, String>> okFuture() {
//        SendResult<String, String> result = mock(SendResult.class);
//        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0);
//        when(result.getRecordMetadata()).thenReturn(metadata);
//        return CompletableFuture.completedFuture(result);
//    }
//
//    @Test
//    void publish_sendsJsonToBookEventsTopic_keyedByMemberId() throws Exception {
//        BookEvent event = BookEvent.issued(7L, "Asha", 2L, "Dune", LocalDate.of(2026, 10, 12), Instant.parse("2026-10-02T10:00:00Z"));
//        // build the future BEFORE the outer when(): okFuture() stubs a mock itself, and Mockito
//        // does not allow stubbing inside another stubbing call (UnfinishedStubbingException)
//        CompletableFuture<SendResult<String, String>> future = okFuture();
//        when(kafkaTemplate.send(eq(TOPIC), eq("7"), anyString())).thenReturn(future);
//
//        producer.publish(event);
//
//        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
//        verify(kafkaTemplate).send(eq(TOPIC), eq("7"), payload.capture());
//        BookEvent roundTrip = mapper.readValue(payload.getValue(), BookEvent.class);
//        assertEquals(event, roundTrip);
//        assertTrue(payload.getValue().contains("\"type\":\"BOOK_ISSUED\""));
//        assertTrue(payload.getValue().contains("\"dueDate\":\"2026-10-12\""));
//    }
//
//    @Test
//    void publish_neverThrows_whenBrokerIsDown() {
//        BookEvent event = BookEvent.returned(7L, "Asha", 2L, "Dune", 30, false, Instant.now());
//        when(kafkaTemplate.send(eq(TOPIC), eq("7"), anyString())).thenThrow(new RuntimeException("broker down"));
//
//        assertDoesNotThrow(() -> producer.publish(event));
//    }
//
//    @Test
//    void publish_neverThrows_whenSendFutureFails() {
//        BookEvent event = BookEvent.returned(7L, "Asha", 2L, "Dune", 30, false, Instant.now());
//        when(kafkaTemplate.send(eq(TOPIC), eq("7"), anyString()))
//                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("timeout")));
//
//        assertDoesNotThrow(() -> producer.onBookEvent(event));
//    }
}
