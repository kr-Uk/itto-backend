package com.itto.backend.infrastructure.oauth;

import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
public class KakaoOAuthVerifier implements OAuthVerifier {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuthUserInfo verify(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL, HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("id")) {
                throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
            }

            String providerUid = String.valueOf(body.get("id"));

            String email = null;
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }

            return new OAuthUserInfo(providerUid, "KAKAO", email);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Kakao OAuth 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }
}
