package com.itto.backend.infrastructure.nice;

import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
public class NiceCryptoUtil {

    private NiceCryptoUtil() {
    }

    /**
     * AES-128-CBC 암호화
     */
    public static String encryptAes128Cbc(String plainText, String key, String iv) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            // key는 16바이트 (128비트)
            keyBytes = Arrays.copyOf(keyBytes, 16);
            ivBytes = Arrays.copyOf(ivBytes, 16);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("AES 암호화 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_CRYPTO_ERROR);
        }
    }

    /**
     * AES-128-CBC 복호화
     */
    public static String decryptAes128Cbc(String encryptedText, String key, String iv) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            keyBytes = Arrays.copyOf(keyBytes, 16);
            ivBytes = Arrays.copyOf(ivBytes, 16);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 복호화 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_CRYPTO_ERROR);
        }
    }

    /**
     * HMAC-SHA256 무결성 검증값 생성
     */
    public static String hmacSha256(String data, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            keyBytes = Arrays.copyOf(keyBytes, 32);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("HMAC-SHA256 생성 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_CRYPTO_ERROR);
        }
    }

    /**
     * SHA-256 해시
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256 해시 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_CRYPTO_ERROR);
        }
    }
}
