package com.elibrary.notification_service.infrastructure.scheduling;

import com.elibrary.notification_service.domain.model.Notification;
import com.elibrary.notification_service.domain.model.NotificationSchedule;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationRepository;
import com.elibrary.notification_service.domain.repository.NotificationScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Materialises scheduled due-soon rows into {@link Notification} records when their time is reached.
 */
@Component
public class ScheduledNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationProcessor.class);

    private final NotificationRepository notificationRepository;
    private final NotificationScheduleRepository scheduleRepository;

    private final int batchSize;

    public ScheduledNotificationProcessor(
        NotificationRepository notificationRepository,
        NotificationScheduleRepository scheduleRepository,
        @Value("${notification.scheduler.batch-size:100}") int batchSize
    ) {
        this.notificationRepository = notificationRepository;
        this.scheduleRepository = scheduleRepository;
        this.batchSize = Math.min(500, Math.max(1, batchSize));
    }

    @Scheduled(fixedDelayString = "${notification.scheduler.fixed-delay-ms:60000}")
    @Transactional
    public void sweep() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationSchedule> due = scheduleRepository.findDueSchedules(now, PageRequest.of(0, batchSize));
        if (due.isEmpty()) {
            return;
        }
        for (NotificationSchedule s : due) {
            Notification n = new Notification();
            n.setUserId(s.getUserId());
            n.setRecordId(s.getRecordId());
            n.setType(s.getNotificationType());
            n.setMessage(reminderMessage(s.getNotificationType(), s.getRecordId()));
            n.setStatus("PENDING");
            notificationRepository.save(n);
            s.setSent(true);
            scheduleRepository.save(s);
        }
        log.debug("Materialised {} scheduled reminder(s)", due.size());
    }

    private static String reminderMessage(NotificationType type, Long recordId) {
        String suffix = recordId != null ? " (borrow record " + recordId + ")." : ".";
        if (type == NotificationType.DUE_SOON_WEEK) {
            return "Your borrowed book is due in about one week" + suffix;
        }
        if (type == NotificationType.DUE_SOON_DAY) {
            return "Your borrowed book is due tomorrow" + suffix;
        }
        return "Library reminder" + suffix;
    }
}
