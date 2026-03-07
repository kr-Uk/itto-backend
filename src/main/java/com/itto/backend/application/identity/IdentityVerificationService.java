package com.itto.backend.application.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itto.backend.infrastructure.nice.NiceProperties;
import com.itto.backend.infrastructure.nice.NiceCryptoUtil;
import com.itto.backend.presentation.dto.identity.IdentityRequestResponse;
import com.itto.backend.presentation.dto.identity.IdentityStatusResponse;
import com.itto.backend.domain.identity.entity.IdentityVerification;
import com.itto.backend.domain.identity.repository.IdentityVerificationRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityVerificationService {

    private final NiceProperties niceProperties;
    private final IdentityVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    /**
     * 본인인증 요청 - NICE 암호화 토큰 생성 및 WebView URL 반환
     */
    @Transactional
    public IdentityRequestResponse requestVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getIdentityVerified()) {
            throw new BusinessException(ErrorCode.IDENTITY_ALREADY_VERIFIED);
        }

        // 1. NICE API 액세스 토큰 발급
        String accessToken = getAccessToken();

        // 2. 암호화 토큰 요청
        String requestNo = UUID.randomUUID().toString().replace("-", "");
        JsonNode cryptoTokenResponse = getCryptoToken(accessToken, requestNo);

        String tokenVersionId = cryptoTokenResponse.path("dataBody").path("token_version_id").asText();
        String siteCode = cryptoTokenResponse.path("dataBody").path("site_code").asText();
        String tokenVal = cryptoTokenResponse.path("dataBody").path("token_val").asText();

        // 3. 대칭키 생성 (요청번호 + 토큰값 기반)
        String key = requestNo.substring(0, 16);
        String iv = requestNo.substring(requestNo.length() - 16);
        String hmacKey = requestNo.substring(0, 32);

        // 4. 요청 데이터 암호화
        String requestData = String.format(
                "{\"requestno\":\"%s\",\"returnurl\":\"%s\",\"errorurl\":\"%s\",\"sitecode\":\"%s\",\"authtype\":\"M\",\"popupyn\":\"N\",\"receivedata\":\"%s\"}",
                requestNo,
                niceProperties.getReturnUrl(),
                niceProperties.getErrorUrl(),
                siteCode,
                userId.toString()
        );

        String encData = NiceCryptoUtil.encryptAes128Cbc(requestData, key, iv);
        String integrityValue = NiceCryptoUtil.hmacSha256(encData, hmacKey);

        // 5. 인증 요청 기록 저장
        IdentityVerification verification = IdentityVerification.builder()
                .user(user)
                .requestNo(requestNo)
                .build();
        verification.updateCryptoData(tokenVersionId, encData, integrityValue);
        verificationRepository.save(verification);

        // 6. WebView에 전달할 URL 생성
        String authUrl = String.format(
                "https://nice.checkplus.co.kr/CheckPlusSa498/checkplus.cb?m=checkplusSecu&token_version_id=%s&enc_data=%s&integrity_value=%s",
                URLEncoder.encode(tokenVersionId, StandardCharsets.UTF_8),
                URLEncoder.encode(encData, StandardCharsets.UTF_8),
                URLEncoder.encode(integrityValue, StandardCharsets.UTF_8)
        );

        return IdentityRequestResponse.builder()
                .requestNo(requestNo)
                .authUrl(authUrl)
                .build();
    }

    /**
     * NICE 인증 성공 콜백 처리
     */
    @Transactional
    public String handleSuccessCallback(String tokenVersionId, String encData, String integrityValue) {
        try {
            // 1. token_version_id로 원본 인증 요청 조회 → requestNo 기반 키 복원
            IdentityVerification verification = verificationRepository.findAll().stream()
                    .filter(v -> tokenVersionId.equals(v.getTokenVersionId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.IDENTITY_NOT_FOUND));

            String requestNo = verification.getRequestNo();
            String key = requestNo.substring(0, 16);
            String iv = requestNo.substring(requestNo.length() - 16);
            String hmacKey = requestNo.substring(0, 32);

            // 2. 무결성 검증
            String computedIntegrity = NiceCryptoUtil.hmacSha256(encData, hmacKey);
            if (!computedIntegrity.equals(integrityValue)) {
                log.error("NICE 콜백 무결성 검증 실패: requestNo={}", requestNo);
                verification.markFailed("INTEGRITY_FAIL", "무결성 검증 실패");
                return buildCallbackHtml(false, "무결성 검증에 실패했습니다.");
            }

            // 3. 복호화
            String decryptedJson = NiceCryptoUtil.decryptAes128Cbc(encData, key, iv);
            JsonNode resultData = objectMapper.readTree(decryptedJson);

            String resultCode = resultData.path("resultcode").asText();
            if (!"0000".equals(resultCode)) {
                verification.markFailed(resultCode, resultData.path("errormsg").asText("인증 실패"));
                return buildCallbackHtml(false, "본인인증에 실패했습니다.");
            }

            // 4. 결과 데이터 추출
            String name = resultData.path("name").asText();
            String phone = resultData.path("mobileno").asText();
            String genderCode = resultData.path("gender").asText();
            String birthDate = resultData.path("birthdate").asText();
            String ci = resultData.path("ci").asText();
            String di = resultData.path("di").asText();
            String carrier = resultData.path("mobileco").asText();

            String gender = "1".equals(genderCode) || "3".equals(genderCode) ? "MALE" : "FEMALE";
            LocalDate birth = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 5. 중복 CI 검사
            User user = verification.getUser();
            if (userRepository.existsByCiAndDeletedAtIsNull(ci) && !ci.equals(user.getCi())) {
                verification.markFailed("DUPLICATE_CI", "이미 가입된 본인인증 정보");
                return buildCallbackHtml(false, "이미 가입된 본인인증 정보입니다.");
            }

            // 6. 인증 결과 저장
            verification.markSuccess(name, phone, gender, birth, ci, di, carrier);
            user.updateIdentityVerification(phone, ci, di);

            return buildCallbackHtml(true, "본인인증이 완료되었습니다.");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("NICE 콜백 처리 실패", e);
            return buildCallbackHtml(false, "본인인증 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * NICE 인증 실패 콜백 처리
     */
    @Transactional
    public String handleFailCallback(String tokenVersionId, String encData) {
        try {
            verificationRepository.findAll().stream()
                    .filter(v -> tokenVersionId.equals(v.getTokenVersionId()))
                    .findFirst()
                    .ifPresent(v -> v.markFailed("USER_CANCEL", "사용자 취소 또는 인증 실패"));
        } catch (Exception e) {
            log.error("NICE 실패 콜백 처리 중 오류", e);
        }
        return buildCallbackHtml(false, "본인인증이 취소되었습니다.");
    }

    /**
     * 본인인증 상태 조회
     */
    public IdentityStatusResponse getVerificationStatus(Long userId) {
        IdentityVerification verification = verificationRepository
                .findTopByUserUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTITY_NOT_FOUND));

        String birthDateStr = null;
        if (verification.getVerifiedBirthDate() != null) {
            birthDateStr = verification.getVerifiedBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        return IdentityStatusResponse.builder()
                .status(verification.getStatus())
                .name(verification.getVerifiedName())
                .gender(verification.getVerifiedGender())
                .birthDate(birthDateStr)
                .phoneNumber(verification.getVerifiedPhone())
                .build();
    }

    /**
     * NICE API 액세스 토큰 발급
     */
    private String getAccessToken() {
        try {
            String credentials = niceProperties.getClientId() + ":" + niceProperties.getClientSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + basicAuth);

            String body = "grant_type=client_credentials&scope=default";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    niceProperties.getAccessTokenUrl(),
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            return response.getBody().path("dataBody").path("access_token").asText();
        } catch (Exception e) {
            log.error("NICE 액세스 토큰 발급 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }

    /**
     * NICE 암호화 토큰 요청
     */
    private JsonNode getCryptoToken(String accessToken, String requestNo) {
        try {
            long currentTimestamp = System.currentTimeMillis() / 1000;
            String authValue = NiceCryptoUtil.sha256(accessToken + ":" + currentTimestamp + ":" + niceProperties.getClientId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("client_id", niceProperties.getClientId());
            headers.set("productID", niceProperties.getProductId());

            Map<String, Object> requestBody = Map.of(
                    "dataHeader", Map.of("CNTY_CD", "ko"),
                    "dataBody", Map.of(
                            "req_dtim", String.valueOf(currentTimestamp),
                            "req_no", requestNo,
                            "enc_mode", "1",
                            "auth_hash", authValue
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    niceProperties.getCryptoTokenUrl(),
                    HttpMethod.POST,
                    request,
                    JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            String resultCode = responseBody.path("dataHeader").path("GW_RSLT_CD").asText();
            if (!"1200".equals(resultCode)) {
                log.error("NICE 암호화 토큰 요청 실패: {}", responseBody);
                throw new BusinessException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
            }

            return responseBody;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("NICE 암호화 토큰 요청 실패", e);
            throw new BusinessException(ErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }

    /**
     * 콜백 결과를 WebView에 전달하는 HTML 생성
     */
    private String buildCallbackHtml(boolean success, String message) {
        String status = success ? "success" : "fail";
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body>
                <script>
                    try {
                        IttoIdentity.postMessage(JSON.stringify({status: '%s', message: '%s'}));
                    } catch(e) {
                        window.IttoIdentity.postMessage(JSON.stringify({status: '%s', message: '%s'}));
                    }
                </script>
                </body>
                </html>
                """, status, message, status, message);
    }
}
