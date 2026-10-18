package com.lms.service;

import com.lms.dto.PageResponse;
import com.lms.dto.ReportDTOs.NotificationResponse;
import com.lms.exception.ResourceNotFoundException;
import com.lms.kafka.BookEvent;
import com.lms.model.Notification;
import com.lms.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;
    @InjectMocks NotificationService service;

    private BookEvent issued() {
        return BookEvent.issued(1L, "Asha", 2L, "Dune", LocalDate.of(2026, 10, 12), Instant.now());
    }

    @Test
    void recordFromEvent_newEvent_savesNotificationWithReadableMessage() {
        BookEvent e = issued();
        when(repository.existsByEventId(e.eventId())).thenReturn(false);

        assertTrue(service.recordFromEvent(e));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals(e.eventId(), n.getEventId());
        assertEquals("BOOK_ISSUED", n.getEventType());
        assertEquals("\"Dune\" was issued to Asha. Due on 2026-10-12.", n.getMessage());
        assertFalse(n.isSeen());
    }

    @Test
    void recordFromEvent_duplicateDelivery_isIgnored() {
        BookEvent e = issued();
        when(repository.existsByEventId(e.eventId())).thenReturn(true);

        assertFalse(service.recordFromEvent(e));
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void buildMessage_returnedWithFeeAndDamage() {
        BookEvent e = BookEvent.returned(1L, "Asha", 2L, "Dune", 530.0, true, Instant.now());
        assertEquals("\"Dune\" was returned by Asha. Fee: \u20B9530.00 (returned damaged).",
                NotificationService.buildMessage(e));
    }

    @Test
    void buildMessage_returnedUndamaged() {
        BookEvent e = BookEvent.returned(1L, "Asha", 2L, "Dune", 30.0, false, Instant.now());
        assertEquals("\"Dune\" was returned by Asha. Fee: \u20B930.00.", NotificationService.buildMessage(e));
    }

    @Test
    void buildMessage_neverExceedsColumnLength() {
        BookEvent e = BookEvent.issued(1L, "A".repeat(600), 2L, "Dune", null, Instant.now());
        assertTrue(NotificationService.buildMessage(e).length() <= 500);
    }

    @Test
    void list_unreadOnlyUsesUnreadQuery() {
        Notification n = new Notification();
        n.setId(1L);
        n.setEventType("BOOK_ISSUED");
        n.setMessage("m");
        when(repository.findBySeenFalseOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        PageResponse<NotificationResponse> res = service.list(true, 0, 20);

        assertEquals(1, res.content().size());
        assertFalse(res.content().get(0).read());
    }

    @Test
    void markRead_flagsNotification() {
        Notification n = new Notification();
        n.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(n));
        when(repository.save(n)).thenReturn(n);

        assertTrue(service.markRead(3L).read());
    }

    @Test
    void markRead_unknownId_throwsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.markRead(9L));
    }
}
