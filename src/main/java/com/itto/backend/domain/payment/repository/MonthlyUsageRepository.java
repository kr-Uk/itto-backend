package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.MonthlyUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyUsageRepository extends JpaRepository<MonthlyUsage, Long> {

    /**
     * Find monthly usage for a user in a specific year-month
     */
    Optional<MonthlyUsage> findByUserUserIdAndYearMonth(Long userId, String yearMonth);
}
