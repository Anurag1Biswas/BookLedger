package com.lms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lms.exception.InvalidEventException;
import com.lms.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

//    @Mock NotificationService notificationService;
//
//    private final ObjectMapper mapper = new ObjectMapper()
//            .registerModule(new JavaTimeModule())
//            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//    private NotificationConsumer consumer;
//
//    @BeforeEach
//    void setUp() {
//        consumer = new NotificationConsumer(mapper, notificationService);
//    }
//
//    @Test
//    void validMessage_isParsedAndHandedToNotificationService() throws Exception {
//        BookEvent event = BookEvent.issued(1L, "Asha", 2L, "Dune", LocalDate.of(2026, 10, 12), Instant.parse("2026-10-02T10:00:00Z"));
//
//        consumer.onMessage(mapper.writeValueAsString(event));
//
//        ArgumentCaptor<BookEvent> captor = ArgumentCaptor.forClass(BookEvent.class);
//        verify(notificationService).recordFromEvent(captor.capture());
//        assertEquals(event, captor.getValue());
//    }
//
//    @Test
//    void malformedJson_isRejectedAsInvalidEvent_soItGoesStraightToTheDeadLetterTopic() {
//        assertThrows(InvalidEventException.class, () -> consumer.onMessage("this is not json"));
//        verify(notificationService, never()).recordFromEvent(any(BookEvent.class));
//    }
//
//    @Test
//    void messageWithoutIdOrType_isRejected() {
//        assertThrows(InvalidEventException.class, () -> consumer.onMessage("{}"));
//        assertThrows(InvalidEventException.class, () -> consumer.onMessage("{\"type\":\"BOOK_ISSUED\"}"));
//    }
}
