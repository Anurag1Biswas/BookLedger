package com.lms.controller;

import com.lms.dto.PageResponse;
import com.lms.dto.ReportDTOs.NotificationResponse;
import com.lms.dto.ReportDTOs.UnreadCount;
import com.lms.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Notifications are created asynchronously by the Kafka consumer; this API only reads / acknowledges them. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCount unreadCount() {
        return new UnreadCount(notificationService.unreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }
}
