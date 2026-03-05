package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserUserIdAndIsActiveTrue(Long userId);

    List<Subscription> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);
}
