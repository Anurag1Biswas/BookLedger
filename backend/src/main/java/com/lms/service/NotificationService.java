package com.lms.service;

import com.lms.dto.PageResponse;
import com.lms.dto.ReportDTOs.NotificationResponse;
import com.lms.exception.ResourceNotFoundException;
import com.lms.kafka.BookEvent;
import com.lms.model.Notification;
import com.lms.repository.NotificationRepository;
import com.lms.util.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** @return true if a notification was created, false if this event had already been processed. */
    @Transactional
    public boolean recordFromEvent(BookEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            return false;
        }
        Notification n = new Notification();
        n.setEventId(event.eventId());
        n.setEventType(event.type().name());
        n.setMemberId(event.memberId());
        n.setBookId(event.bookId());
        n.setMessage(buildMessage(event));
        repository.save(n);
        return true;
    }

    static String buildMessage(BookEvent e) {
        String title = e.bookTitle() == null ? "A book" : "\"" + e.bookTitle() + "\"";
        String member = e.memberName() == null ? "a member" : e.memberName();
        String text = switch (e.type()) {
            case BOOK_ISSUED -> title + " was issued to " + member
                    + (e.dueDate() != null ? ". Due on " + e.dueDate() + "." : ".");
            case BOOK_RETURNED -> title + " was returned by " + member
                    + (e.billAmount() != null ? String.format(Locale.ROOT, ". Fee: \u20B9%.2f", e.billAmount()) : "")
                    + (Boolean.TRUE.equals(e.damaged()) ? " (returned damaged)." : ".");
        };
        return text.length() > 500 ? text.substring(0, 497) + "..." : text;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), PageUtils.MAX_PAGE_SIZE));
        Page<Notification> result = unreadOnly
                ? repository.findBySeenFalseOrderByCreatedAtDescIdDesc(pageable)
                : repository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        return PageResponse.from(result, NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countBySeenFalse();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        n.setSeen(true);
        return toResponse(repository.save(n));
    }

    @Transactional
    public int markAllRead() {
        return repository.markAllSeen();
    }

    static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventType(), n.getMemberId(), n.getBookId(),
                n.getMessage(), n.getCreatedAt(), n.isSeen());
    }
}
