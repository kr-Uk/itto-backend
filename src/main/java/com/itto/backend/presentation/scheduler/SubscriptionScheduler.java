package com.itto.backend.presentation.scheduler;

import com.itto.backend.application.payment.SubscriptionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionBatchService subscriptionBatchService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runSubscriptionExpiration() {
        log.info("[스케줄러] 구독 만료 처리 시작");
        long start = System.currentTimeMillis();

        try {
            int count = subscriptionBatchService.processExpiredSubscriptions();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[스케줄러] 구독 만료 처리 완료: {}건, {}ms", count, elapsed);
        } catch (Exception e) {
            log.error("[스케줄러] 구독 만료 처리 실패", e);
        }
    }
}
