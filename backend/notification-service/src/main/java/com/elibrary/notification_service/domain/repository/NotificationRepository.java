package com.elibrary.notification_service.domain.repository;

import com.elibrary.notification_service.domain.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
