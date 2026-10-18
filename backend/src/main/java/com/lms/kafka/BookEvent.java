package com.lms.kafka;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The message published to the "book-events" topic (JSON, key = memberId).
 * It is also used as the in-process Spring application event raised by IssueService,
 * so there is exactly one definition of what "a book was issued/returned" means.
 *
 * eventId is the idempotency key: consumers must tolerate redelivery.
 * dueDate is set for BOOK_ISSUED; billAmount/damaged are set for BOOK_RETURNED.
 */
public record BookEvent(
        String eventId,
        BookEventType type,
        Long memberId,
        String memberName,
        Long bookId,
        String bookTitle,
        LocalDate dueDate,
        Double billAmount,
        Boolean damaged,
        Instant occurredAt) {

    public static BookEvent issued(Long memberId, String memberName, Long bookId, String bookTitle,
                                   LocalDate dueDate, Instant at) {
        return new BookEvent(UUID.randomUUID().toString(), BookEventType.BOOK_ISSUED,
                memberId, memberName, bookId, bookTitle, dueDate, null, null, at);
    }

    public static BookEvent returned(Long memberId, String memberName, Long bookId, String bookTitle,
                                     double billAmount, boolean damaged, Instant at) {
        return new BookEvent(UUID.randomUUID().toString(), BookEventType.BOOK_RETURNED,
                memberId, memberName, bookId, bookTitle, null, billAmount, damaged, at);
    }
}
