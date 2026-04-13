package com.elibrary.notification_service.application;

import com.elibrary.notification_service.domain.model.Notification;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationRepository;
import com.elibrary.notification_service.integration.dto.BookBorrowedMessage;
import com.elibrary.notification_service.integration.dto.BookOverdueMessage;
import com.elibrary.notification_service.integration.dto.BookRenewedMessage;
import com.elibrary.notification_service.integration.dto.BookReturnedMessage;
import com.elibrary.notification_service.integration.dto.FineAppliedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DueReminderScheduleService dueReminderScheduleService;

    @InjectMocks
    private NotificationDispatchService notificationDispatchService;

    @Test
    void handleBorrowed_persistsPendingBorrowedNotification() {
        BookBorrowedMessage message = new BookBorrowedMessage();
        message.setUserId(5L);
        message.setRecordId(77L);
        message.setDueDate(LocalDate.of(2026, 8, 15));

        notificationDispatchService.handleBorrowed(message);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(NotificationType.BOOK_BORROWED, saved.getType());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("Book borrowed: record 77", saved.getMessage());
        verify(dueReminderScheduleService).scheduleRemindersForBorrow(eq(5L), eq(77L), eq(LocalDate.of(2026, 8, 15)));
    }

    @Test
    void handleReturned_cancelsPendingSchedules() {
        BookReturnedMessage message = new BookReturnedMessage();
        message.setUserId(9L);
        message.setRecordId(55L);

        notificationDispatchService.handleReturned(message);

        verify(dueReminderScheduleService).cancelPendingForRecord(55L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleRenewed_reschedulesReminders() {
        BookRenewedMessage message = new BookRenewedMessage();
        message.setUserId(3L);
        message.setRecordId(88L);
        message.setNewDueDate(LocalDate.of(2026, 9, 1));

        notificationDispatchService.handleRenewed(message);

        verify(dueReminderScheduleService).rescheduleAfterRenew(eq(3L), eq(88L), eq(LocalDate.of(2026, 9, 1)));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void handleOverdue_includesDaysOverdueInMessage() {
        BookOverdueMessage message = new BookOverdueMessage();
        message.setUserId(7L);
        message.setRecordId(99L);
        message.setDaysOverdue(4);

        notificationDispatchService.handleOverdue(message);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(NotificationType.BOOK_OVERDUE, saved.getType());
        assertEquals("Overdue: record 99 (4 days)", saved.getMessage());
    }

    @Test
    void handleFineApplied_buildsFineAppliedNotification() {
        FineAppliedMessage message = new FineAppliedMessage();
        message.setUserId(3L);
        message.setRecordId(22L);
        message.setAmount(12.5);

        notificationDispatchService.handleFineApplied(message);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(NotificationType.FINE_APPLIED, saved.getType());
        assertEquals("Fine applied: 12.5 for record 22", saved.getMessage());
    }
}
