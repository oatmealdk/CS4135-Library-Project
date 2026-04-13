package com.elibrary.notification_service.application;

import com.elibrary.notification_service.domain.model.Notification;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationRepository;
import com.elibrary.notification_service.integration.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final DueReminderScheduleService dueReminderScheduleService;

    public NotificationDispatchService(
        NotificationRepository notificationRepository,
        DueReminderScheduleService dueReminderScheduleService
    ) {
        this.notificationRepository = notificationRepository;
        this.dueReminderScheduleService = dueReminderScheduleService;
    }

    @Transactional
    public void handleBorrowed(BookBorrowedMessage m) {
        log.debug("Book borrowed message for recordId={}", m.getRecordId());
        notificationRepository.save(build(m.getUserId(), m.getRecordId(), NotificationType.BOOK_BORROWED,
            "Book borrowed: record " + m.getRecordId()));
        dueReminderScheduleService.scheduleRemindersForBorrow(m.getUserId(), m.getRecordId(), m.getDueDate());
    }

    @Transactional
    public void handleRenewed(BookRenewedMessage m) {
        log.debug("Book renewed message for recordId={}", m.getRecordId());
        dueReminderScheduleService.rescheduleAfterRenew(m.getUserId(), m.getRecordId(), m.getNewDueDate());
        notificationRepository.save(build(m.getUserId(), m.getRecordId(), NotificationType.BOOK_RENEWED,
            "Renewed: record " + m.getRecordId()));
    }

    @Transactional
    public void handleReturned(BookReturnedMessage m) {
        log.debug("Book returned message for recordId={}", m.getRecordId());
        dueReminderScheduleService.cancelPendingForRecord(m.getRecordId());
        notificationRepository.save(build(m.getUserId(), m.getRecordId(), NotificationType.BOOK_RETURNED,
            "Return confirmed: record " + m.getRecordId()));
    }

    @Transactional
    public void handleOverdue(BookOverdueMessage m) {
        log.debug("Book overdue message for recordId={}", m.getRecordId());
        notificationRepository.save(build(m.getUserId(), m.getRecordId(), NotificationType.BOOK_OVERDUE,
            "Overdue: record " + m.getRecordId() + " (" + m.getDaysOverdue() + " days)"));
    }

    @Transactional
    public void handleFineApplied(FineAppliedMessage m) {
        log.debug("Fine applied message for recordId={}", m.getRecordId());
        notificationRepository.save(build(m.getUserId(), m.getRecordId(), NotificationType.FINE_APPLIED,
            "Fine applied: " + m.getAmount() + " for record " + m.getRecordId()));
    }

    private static Notification build(Long userId, Long recordId, NotificationType type, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setRecordId(recordId);
        n.setType(type);
        n.setMessage(message);
        n.setStatus("PENDING");
        return n;
    }
}
