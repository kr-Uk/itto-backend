package com.itto.backend.domain.contact.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PhoneNumberHashUtil {

    private PhoneNumberHashUtil() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("전화번호는 null이거나 빈 문자열일 수 없습니다");
        }

        String cleaned = phoneNumber.replaceAll("[\\s\\-()]", "");

        if (cleaned.startsWith("+82")) {
            cleaned = "0" + cleaned.substring(3);
        }

        if (cleaned.startsWith("0")) {
            cleaned = "+82" + cleaned.substring(1);
        }

        return cleaned;
    }

    public static String hash(String normalizedNumber) {
        if (normalizedNumber == null || normalizedNumber.isBlank()) {
            throw new IllegalArgumentException("해시할 문자열은 null이거나 빈 문자열일 수 없습니다");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalizedNumber.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }

    public static String normalizeAndHash(String phoneNumber) {
        return hash(normalize(phoneNumber));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
