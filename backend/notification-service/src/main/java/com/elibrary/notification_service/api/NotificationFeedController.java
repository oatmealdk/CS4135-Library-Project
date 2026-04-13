package com.elibrary.notification_service.api;

import com.elibrary.notification_service.domain.model.Notification;
import com.elibrary.notification_service.domain.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationFeedController {

    private final NotificationRepository notificationRepository;

    public NotificationFeedController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> recent(@RequestParam(defaultValue = "50") int limit) {
        int capped = Math.min(Math.max(limit, 1), 200);
        return ResponseEntity.ok(
            notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, capped)));
    }

    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<List<Notification>> recentForUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "20") int limit
    ) {
        int capped = Math.min(Math.max(limit, 1), 200);
        return ResponseEntity.ok(
            notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, capped)));
    }
}
