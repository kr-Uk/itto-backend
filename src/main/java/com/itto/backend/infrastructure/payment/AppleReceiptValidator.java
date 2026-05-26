package com.itto.backend.infrastructure.payment;

import com.itto.backend.application.payment.port.ReceiptValidationPort.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * App Store 영수증 검증 stub.
 *
 * 실제 Apple App Store Server API 연동 구현은 결제 도메인 담당자가 채워 넣어야 한다.
 * 현재는 항상 invalid를 반환하므로 프로덕션 환경에서는 모든 영수증이 거부된다.
 * 개발 환경에서는 itto.payment.skip-receipt-validation=true로 우회 가능.
 */
@Slf4j
@Component
public class AppleReceiptValidator {

    private static final String STORE = "APPLE";

    public ValidationResult validatePurchase(String receiptToken) {
        log.warn("AppleReceiptValidator.validatePurchase stub 호출 — 실제 검증 미구현 (영수증 거부)");
        return ValidationResult.invalid(STORE);
    }

    public ValidationResult validateSubscription(String receiptToken) {
        log.warn("AppleReceiptValidator.validateSubscription stub 호출 — 실제 검증 미구현 (영수증 거부)");
        return ValidationResult.invalid(STORE);
    }
}
