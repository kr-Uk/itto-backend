package com.itto.backend.domain.notification.repository;

import com.itto.backend.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a user, ordered by creation time (most recent first)
     */
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all unread notifications for a user
     */
    List<Notification> findByUserUserIdAndIsReadFalse(Long userId);

    /**
     * Count unread notifications for a user
     */
    long countByUserUserIdAndIsReadFalse(Long userId);
}
