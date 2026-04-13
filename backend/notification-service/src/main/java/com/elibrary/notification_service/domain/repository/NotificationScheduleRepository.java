package com.elibrary.notification_service.domain.repository;

import com.elibrary.notification_service.domain.model.NotificationSchedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    @Query("select s from NotificationSchedule s where s.sent = false and s.cancelled = false and s.scheduledFor <= :now order by s.scheduledFor asc")
    List<NotificationSchedule> findDueSchedules(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("update NotificationSchedule s set s.cancelled = true where s.recordId = :recordId and s.sent = false and s.cancelled = false")
    int cancelPendingForRecord(@Param("recordId") Long recordId);
}
