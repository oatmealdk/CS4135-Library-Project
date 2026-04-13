package com.elibrary.notification_service.application;

import com.elibrary.notification_service.domain.model.NotificationSchedule;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DueReminderScheduleServiceTest {

    @Mock
    private NotificationScheduleRepository scheduleRepository;

    private DueReminderScheduleService service;

    @BeforeEach
    void setUp() {
        service = new DueReminderScheduleService(scheduleRepository, 9);
    }

    @Test
    void scheduleRemindersForBorrow_savesWeekAndDayWhenLoanIsLong() {
        LocalDate due = LocalDate.now().plusDays(30);
        when(scheduleRepository.save(any(NotificationSchedule.class))).thenAnswer(i -> i.getArgument(0));

        service.scheduleRemindersForBorrow(1L, 2L, due);

        ArgumentCaptor<NotificationSchedule> captor = ArgumentCaptor.forClass(NotificationSchedule.class);
        verify(scheduleRepository, times(2)).save(captor.capture());
        var types = captor.getAllValues().stream().map(NotificationSchedule::getNotificationType).toList();
        assertTrue(types.contains(NotificationType.DUE_SOON_WEEK));
        assertTrue(types.contains(NotificationType.DUE_SOON_DAY));
    }

    @Test
    void cancelPendingForRecord_delegatesToRepository() {
        when(scheduleRepository.cancelPendingForRecord(9L)).thenReturn(2);
        service.cancelPendingForRecord(9L);
        verify(scheduleRepository).cancelPendingForRecord(9L);
    }

    @Test
    void rescheduleAfterRenew_cancelsThenSchedules() {
        LocalDate due = LocalDate.now().plusDays(14);
        when(scheduleRepository.save(any(NotificationSchedule.class))).thenAnswer(i -> i.getArgument(0));

        service.rescheduleAfterRenew(5L, 6L, due);

        verify(scheduleRepository).cancelPendingForRecord(6L);
        verify(scheduleRepository, atLeastOnce()).save(any(NotificationSchedule.class));
    }
}
