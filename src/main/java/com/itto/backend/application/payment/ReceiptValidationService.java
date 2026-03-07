package com.itto.backend.application.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 영수증 검증 서비스
 * App Store 및 Google Play 영수증 검증을 담당
 *
 * [보안 주의] itto.payment.skip-receipt-validation=true 일 때만 stub 모드 허용.
 * 프로덕션 배포 시 반드시 false로 설정하고 실제 스토어 API 연동 필요.
 */
@Slf4j
@Service
public class ReceiptValidationService {

    /**
     * true = 개발/테스트 모드 (영수증 검증 스킵, 테스트 토큰 허용)
     * false = 프로덕션 모드 (실제 스토어 API 검증 필수, 미구현 시 모든 영수증 거부)
     */
    @Value("${itto.payment.skip-receipt-validation:false}")
    private boolean skipReceiptValidation;

    /**
     * 매듭 구매 영수증 검증
     */
    public ReceiptValidationResult validatePurchaseReceipt(String store, String receiptToken) {
        log.info("영수증 검증 시작 - 스토어: {}, 토큰 길이: {}", store, receiptToken != null ? receiptToken.length() : 0);

        // 빈 토큰은 항상 거부
        if (receiptToken == null || receiptToken.isBlank()) {
            log.warn("영수증 토큰이 비어있습니다");
            return invalidResult(store);
        }

        // 테스트 토큰은 항상 거부 (프론트에서 하드코딩된 값 차단)
        if ("test_receipt_token".equals(receiptToken)) {
            if (skipReceiptValidation) {
                log.warn("[DEV 모드] 테스트 토큰 허용 - 프로덕션에서는 반드시 차단해야 합니다");
                return devModeResult(store, "purchase");
            }
            log.warn("테스트 영수증 토큰이 거부되었습니다");
            return invalidResult(store);
        }

        if ("APPLE".equals(store)) {
            return validateApplePurchase(receiptToken);
        } else if ("GOOGLE".equals(store)) {
            return validateGooglePurchase(receiptToken);
        }

        log.warn("알 수 없는 스토어: {}", store);
        return invalidResult(store);
    }

    /**
     * 구독 영수증 검증
     */
    public ReceiptValidationResult validateSubscriptionReceipt(String store, String receiptToken) {
        log.info("구독 영수증 검증 시작 - 스토어: {}, 토큰 길이: {}", store, receiptToken != null ? receiptToken.length() : 0);

        // 빈 토큰은 항상 거부
        if (receiptToken == null || receiptToken.isBlank()) {
            log.warn("구독 영수증 토큰이 비어있습니다");
            return invalidResult(store);
        }

        // 테스트 토큰 차단
        if ("test_receipt_token".equals(receiptToken)) {
            if (skipReceiptValidation) {
                log.warn("[DEV 모드] 테스트 구독 토큰 허용 - 프로덕션에서는 반드시 차단해야 합니다");
                return devModeResult(store, "subscription");
            }
            log.warn("테스트 구독 영수증 토큰이 거부되었습니다");
            return invalidResult(store);
        }

        if ("APPLE".equals(store)) {
            return validateAppleSubscription(receiptToken);
        } else if ("GOOGLE".equals(store)) {
            return validateGoogleSubscription(receiptToken);
        }

        log.warn("알 수 없는 스토어: {}", store);
        return invalidResult(store);
    }

    // ==================== Apple 검증 ====================

    private ReceiptValidationResult validateApplePurchase(String receiptToken) {
        // TODO: Apple App Store Server API v2 호출
        // https://developer.apple.com/documentation/appstoreserverapi
        // 1. JWT 생성 (App Store Connect API Key 사용)
        // 2. POST /inApps/v1/transactions/{transactionId} 또는 verifyReceipt
        // 3. 응답에서 productId, transactionId, purchaseDate 파싱
        // 4. 중복 구매 체크 (같은 transactionId로 이미 처리했는지)

        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Apple 영수증 검증 스킵");
            return devModeResult("APPLE", "purchase");
        }

        log.error("Apple 영수증 검증 미구현 - 프로덕션에서 거부됨");
        return invalidResult("APPLE");
    }

    private ReceiptValidationResult validateAppleSubscription(String receiptToken) {
        // TODO: Apple App Store Server API v2 구독 검증
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Apple 구독 영수증 검증 스킵");
            return devModeResult("APPLE", "subscription");
        }
        log.error("Apple 구독 영수증 검증 미구현 - 프로덕션에서 거부됨");
        return invalidResult("APPLE");
    }

    // ==================== Google 검증 ====================

    private ReceiptValidationResult validateGooglePurchase(String receiptToken) {
        // TODO: Google Play Developer API 호출
        // https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.products/get
        // 1. Google 서비스 계정 인증 (JSON key file)
        // 2. GET /androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{token}
        // 3. 응답에서 purchaseState, orderId 확인
        // 4. purchaseState == 0 (구매 완료) 확인
        // 5. 중복 구매 체크

        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Google 영수증 검증 스킵");
            return devModeResult("GOOGLE", "purchase");
        }

        log.error("Google 영수증 검증 미구현 - 프로덕션에서 거부됨");
        return invalidResult("GOOGLE");
    }

    private ReceiptValidationResult validateGoogleSubscription(String receiptToken) {
        // TODO: Google Play Developer API 구독 검증
        // https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptions/get
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Google 구독 영수증 검증 스킵");
            return devModeResult("GOOGLE", "subscription");
        }
        log.error("Google 구독 영수증 검증 미구현 - 프로덕션에서 거부됨");
        return invalidResult("GOOGLE");
    }

    // ==================== Helper ====================

    private ReceiptValidationResult invalidResult(String store) {
        return ReceiptValidationResult.builder()
                .isValid(false)
                .transactionId(null)
                .productId(null)
                .store(store)
                .build();
    }

    private ReceiptValidationResult devModeResult(String store, String type) {
        return ReceiptValidationResult.builder()
                .isValid(true)
                .transactionId("dev_" + type + "_" + System.currentTimeMillis())
                .productId("com.itto.dev." + type)
                .store(store)
                .build();
    }

    /**
     * 영수증 검증 결과
     */
    @Getter
    @Builder
    public static class ReceiptValidationResult {
        /**
         * 검증 성공 여부
         */
        private final boolean isValid;

        /**
         * 트랜잭션 ID (영수증에서 파싱)
         */
        private final String transactionId;

        /**
         * 상품 ID (영수증에서 파싱)
         */
        private final String productId;

        /**
         * 스토어 종류
         */
        private final String store;
    }
}
