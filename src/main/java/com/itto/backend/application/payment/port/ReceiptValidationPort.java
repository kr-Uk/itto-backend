package com.itto.backend.application.payment.port;

public interface ReceiptValidationPort {

    ValidationResult validatePurchase(String store, String receiptToken);

    ValidationResult validateSubscription(String store, String receiptToken);

    record ValidationResult(boolean valid, String transactionId) {}
}
