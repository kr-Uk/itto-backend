package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Find active subscription for a user
     */
    Optional<Subscription> findByUserUserIdAndIsActiveTrue(Long userId);
}
