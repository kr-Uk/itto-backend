package com.itto.backend.application.payment.port;

/**
 * 영수증 검증 포트 (application → infrastructure 의존 역전)
 *
 * 이 인터페이스를 통해 application 레이어는 실제 스토어 API 구현에 의존하지 않고
 * 영수증 검증 기능을 사용할 수 있습니다.
 */
public interface ReceiptValidationPort {

    /**
     * 인앱 구매(소비성 상품) 영수증 검증
     *
     * @param store 스토어 종류 ("APPLE" 또는 "GOOGLE")
     * @param receiptToken 클라이언트에서 전달받은 영수증 토큰
     * @return 검증 결과
     */
    ValidationResult validatePurchase(String store, String receiptToken);

    /**
     * 구독 영수증 검증
     *
     * @param store 스토어 종류 ("APPLE" 또는 "GOOGLE")
     * @param receiptToken 클라이언트에서 전달받은 영수증 토큰
     * @return 검증 결과
     */
    ValidationResult validateSubscription(String store, String receiptToken);

    /**
     * 영수증 검증 결과
     *
     * @param valid      검증 성공 여부
     * @param transactionId 스토어에서 발급한 트랜잭션/주문 ID (Apple: transactionId, Google: orderId)
     * @param productId  검증된 상품 ID (스토어에서 반환한 값)
     * @param store      검증한 스토어 ("APPLE" 또는 "GOOGLE")
     */
    record ValidationResult(
            boolean valid,
            String transactionId,
            String productId,
            String store
    ) {
        /** 간편 생성 - transactionId만 있는 경우 */
        public ValidationResult(boolean valid, String transactionId) {
            this(valid, transactionId, null, null);
        }

        /** 실패 결과 생성 */
        public static ValidationResult invalid(String store) {
            return new ValidationResult(false, null, null, store);
        }
    }
}
