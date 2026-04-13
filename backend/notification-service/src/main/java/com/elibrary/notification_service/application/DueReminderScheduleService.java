package com.elibrary.notification_service.application;

import com.elibrary.notification_service.domain.model.NotificationSchedule;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Schedules 7-day and 1-day-before-due reminders (strategic doc: NotificationSchedule / due-soon flows).
 */
@Service
public class DueReminderScheduleService {

    private static final Logger log = LoggerFactory.getLogger(DueReminderScheduleService.class);

    private final NotificationScheduleRepository scheduleRepository;

    private final int reminderHour;

    public DueReminderScheduleService(
        NotificationScheduleRepository scheduleRepository,
        @Value("${notification.reminder.morning-hour:9}") int reminderHour
    ) {
        this.scheduleRepository = scheduleRepository;
        this.reminderHour = reminderHour;
    }

    @Transactional
    public void scheduleRemindersForBorrow(Long userId, Long recordId, LocalDate dueDate) {
        if (userId == null || recordId == null || dueDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalTime time = LocalTime.of(Math.min(23, Math.max(0, reminderHour)), 0);

        LocalDateTime weekReminder = LocalDateTime.of(dueDate.minusDays(7), time);
        if (!weekReminder.isBefore(now)) {
            persistSchedule(userId, recordId, weekReminder, NotificationType.DUE_SOON_WEEK);
        }

        LocalDateTime dayReminder = LocalDateTime.of(dueDate.minusDays(1), time);
        if (dayReminder.isBefore(now) && dueDate.equals(today.plusDays(1))) {
            dayReminder = now.plusSeconds(5);
        } else if (dayReminder.isBefore(now)) {
            dayReminder = null;
        }
        if (dayReminder != null) {
            persistSchedule(userId, recordId, dayReminder, NotificationType.DUE_SOON_DAY);
        }
    }

    @Transactional
    public void cancelPendingForRecord(Long recordId) {
        if (recordId == null) {
            return;
        }
        int n = scheduleRepository.cancelPendingForRecord(recordId);
        if (n > 0) {
            log.debug("Cancelled {} pending reminder schedule(s) for recordId={}", n, recordId);
        }
    }

    /**
     * After renewal: drop old pending reminders and schedule from the new due date.
     */
    @Transactional
    public void rescheduleAfterRenew(Long userId, Long recordId, LocalDate newDueDate) {
        cancelPendingForRecord(recordId);
        scheduleRemindersForBorrow(userId, recordId, newDueDate);
    }

    private void persistSchedule(Long userId, Long recordId, LocalDateTime when, NotificationType type) {
        NotificationSchedule s = new NotificationSchedule();
        s.setUserId(userId);
        s.setRecordId(recordId);
        s.setScheduledFor(when);
        s.setNotificationType(type);
        s.setSent(false);
        s.setCancelled(false);
        scheduleRepository.save(s);
        log.debug("Scheduled {} for userId={} recordId={} at {}", type, userId, recordId, when);
    }
}
