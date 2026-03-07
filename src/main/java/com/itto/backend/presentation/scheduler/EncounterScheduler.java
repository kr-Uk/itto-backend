package com.itto.backend.presentation.scheduler;

import com.itto.backend.application.relationship.EncounterBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncounterScheduler {

    private final EncounterBatchService encounterBatchService;

    /**
     * 스침 판정 배치 - 5분마다 실행
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void runEncounterDetection() {
        log.info("[스케줄러] 스침 판정 배치 시작");
        long start = System.currentTimeMillis();

        try {
            int count = encounterBatchService.detectEncounters();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[스케줄러] 스침 판정 배치 완료: {}건, {}ms", count, elapsed);
        } catch (Exception e) {
            log.error("[스케줄러] 스침 판정 배치 실패", e);
        }
    }

    /**
     * 만료 처리 배치 - 30분마다 실행
     */
    @Scheduled(fixedRate = 30 * 60 * 1000, initialDelay = 2 * 60 * 1000)
    public void runExpirationProcessing() {
        log.info("[스케줄러] 만료 처리 배치 시작");
        long start = System.currentTimeMillis();

        try {
            int count = encounterBatchService.processExpirations();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[스케줄러] 만료 처리 배치 완료: {}건, {}ms", count, elapsed);
        } catch (Exception e) {
            log.error("[스케줄러] 만료 처리 배치 실패", e);
        }
    }
}
