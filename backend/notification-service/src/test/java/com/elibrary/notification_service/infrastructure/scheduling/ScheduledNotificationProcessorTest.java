package com.elibrary.notification_service.infrastructure.scheduling;

import com.elibrary.notification_service.domain.model.Notification;
import com.elibrary.notification_service.domain.model.NotificationSchedule;
import com.elibrary.notification_service.domain.model.NotificationType;
import com.elibrary.notification_service.domain.repository.NotificationRepository;
import com.elibrary.notification_service.domain.repository.NotificationScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledNotificationProcessorTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationScheduleRepository scheduleRepository;

    private ScheduledNotificationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ScheduledNotificationProcessor(notificationRepository, scheduleRepository, 100);
    }

    @Test
    void sweep_createsNotificationAndMarksScheduleSent() {
        NotificationSchedule row = new NotificationSchedule();
        row.setUserId(3L);
        row.setRecordId(99L);
        row.setNotificationType(NotificationType.DUE_SOON_DAY);
        row.setScheduledFor(LocalDateTime.now().minusMinutes(1));
        row.setSent(false);
        row.setCancelled(false);

        when(scheduleRepository.findDueSchedules(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(row));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
        when(scheduleRepository.save(any(NotificationSchedule.class))).thenAnswer(i -> i.getArgument(0));

        processor.sweep();

        ArgumentCaptor<Notification> nCap = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(nCap.capture());
        assertEquals(NotificationType.DUE_SOON_DAY, nCap.getValue().getType());
        assertEquals(99L, nCap.getValue().getRecordId());
        assertTrue(nCap.getValue().getMessage().contains("tomorrow"));

        ArgumentCaptor<NotificationSchedule> sCap = ArgumentCaptor.forClass(NotificationSchedule.class);
        verify(scheduleRepository).save(sCap.capture());
        assertTrue(sCap.getValue().isSent());
        verify(scheduleRepository).findDueSchedules(any(LocalDateTime.class), any(Pageable.class));
    }
}
