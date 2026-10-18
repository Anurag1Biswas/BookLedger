package com.lms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** In-app notification created asynchronously by the Kafka consumer from a BookEvent. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Event id from Kafka; unique so a redelivered message never creates a duplicate notification. */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean seen = false;

    public Notification() {}

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Long getMemberId() { return memberId; }
    public Long getBookId() { return bookId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isSeen() { return seen; }

    public void setId(Long id) { this.id = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSeen(boolean seen) { this.seen = seen; }
}
