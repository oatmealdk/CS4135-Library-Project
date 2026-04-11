package com.elibrary.notification_service.infrastructure.scheduling;

import com.elibrary.notification_service.domain.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hook for reminder delivery / outbound channels. Extend as needed.
 */
@Component
public class ScheduledNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationProcessor.class);

    private final NotificationRepository notificationRepository;

    public ScheduledNotificationProcessor(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:60000}")
    public void sweep() {
        long count = notificationRepository.count();
        log.trace("Notification repository row count: {}", count);
    }
}
