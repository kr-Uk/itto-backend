package com.itto.backend.domain.notification.repository;

import com.itto.backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find notifications for a user with pagination
     */
    Page<Notification> findByUserUserId(Long userId, Pageable pageable);

    /**
     * Find notifications by IDs and user ID (for bulk operations)
     */
    List<Notification> findByIdInAndUserUserId(List<Long> ids, Long userId);

    /**
     * Delete all notifications for a user
     */
    void deleteByUserUserId(Long userId);
}
