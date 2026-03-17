package com.itto.backend.application.payment;

import com.itto.backend.application.payment.port.ReceiptValidationPort;
import com.itto.backend.infrastructure.payment.AppleReceiptValidator;
import com.itto.backend.infrastructure.payment.GoogleReceiptValidator;
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

    private final AppleReceiptValidator appleReceiptValidator;
    private final GoogleReceiptValidator googleReceiptValidator;

    public ReceiptValidationService(AppleReceiptValidator appleReceiptValidator,
                                     GoogleReceiptValidator googleReceiptValidator) {
        this.appleReceiptValidator = appleReceiptValidator;
        this.googleReceiptValidator = googleReceiptValidator;
    }

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
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Apple 영수증 검증 스킵");
            return devModeResult("APPLE", "purchase");
        }

        ReceiptValidationPort.ValidationResult portResult =
                appleReceiptValidator.validatePurchase(receiptToken);

        return toResult("APPLE", portResult);
    }

    private ReceiptValidationResult validateAppleSubscription(String receiptToken) {
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Apple 구독 영수증 검증 스킵");
            return devModeResult("APPLE", "subscription");
        }

        ReceiptValidationPort.ValidationResult portResult =
                appleReceiptValidator.validateSubscription(receiptToken);

        return toResult("APPLE", portResult);
    }

    // ==================== Google 검증 ====================

    private ReceiptValidationResult validateGooglePurchase(String receiptToken) {
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Google 영수증 검증 스킵");
            return devModeResult("GOOGLE", "purchase");
        }

        ReceiptValidationPort.ValidationResult portResult =
                googleReceiptValidator.validatePurchase(receiptToken);

        return toResult("GOOGLE", portResult);
    }

    private ReceiptValidationResult validateGoogleSubscription(String receiptToken) {
        if (skipReceiptValidation) {
            log.warn("[DEV 모드] Google 구독 영수증 검증 스킵");
            return devModeResult("GOOGLE", "subscription");
        }

        ReceiptValidationPort.ValidationResult portResult =
                googleReceiptValidator.validateSubscription(receiptToken);

        return toResult("GOOGLE", portResult);
    }

    // ==================== Helper ====================

    private ReceiptValidationResult toResult(String store, ReceiptValidationPort.ValidationResult portResult) {
        return ReceiptValidationResult.builder()
                .isValid(portResult.valid())
                .transactionId(portResult.transactionId())
                .productId(portResult.productId())
                .store(store)
                .build();
    }

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
