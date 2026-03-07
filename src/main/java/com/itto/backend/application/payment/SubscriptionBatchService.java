package com.itto.backend.application.payment;

import com.itto.backend.domain.payment.entity.Subscription;
import com.itto.backend.domain.payment.repository.SubscriptionRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionBatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public int processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expired = subscriptionRepository
                .findByIsActiveTrueAndExpiresAtBefore(now);

        int processed = 0;
        for (Subscription subscription : expired) {
            try {
                subscription.expire();
                User user = subscription.getUser();
                user.updatePremium(false);
                processed++;
            } catch (Exception e) {
                log.error("[구독만료] 처리 실패: subscriptionId={}",
                        subscription.getId(), e);
            }
        }

        if (processed > 0) {
            log.info("[구독만료] 만료 처리 완료: {}건 / {}건", processed, expired.size());
        }
        return processed;
    }
}
