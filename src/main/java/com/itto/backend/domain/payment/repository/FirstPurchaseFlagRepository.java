package com.itto.backend.domain.payment.repository;

import com.itto.backend.domain.payment.entity.FirstPurchaseFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirstPurchaseFlagRepository extends JpaRepository<FirstPurchaseFlag, Long> {

    /**
     * Check if user has made first purchase for a specific package
     */
    boolean existsByUserUserIdAndPackageName(Long userId, String packageName);
}
