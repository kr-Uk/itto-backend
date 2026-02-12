package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /**
     * Find all purchases for a user
     */
    List<Purchase> findByUserUserId(Long userId);
}
