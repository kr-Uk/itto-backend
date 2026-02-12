package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.KnotTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnotTransactionRepository extends JpaRepository<KnotTransaction, Long> {

    /**
     * Find all knot transactions for a user, ordered by creation time (most recent first)
     */
    List<KnotTransaction> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}
