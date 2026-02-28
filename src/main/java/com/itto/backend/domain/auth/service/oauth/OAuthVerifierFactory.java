package com.itto.backend.domain.auth.service.oauth;

import com.itto.backend.domain.auth.config.OAuthProperties;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthVerifierFactory {

    private final OAuthProperties oAuthProperties;

    public OAuthVerifier getVerifier(String provider) {
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> new GoogleOAuthVerifier(oAuthProperties.getGoogleClientId());
            case "KAKAO" -> new KakaoOAuthVerifier();
            case "APPLE" -> new AppleOAuthVerifier(oAuthProperties.getAppleClientId());
            default -> throw new BusinessException(ErrorCode.UNSUPPORTED_PROVIDER);
        };
    }
}
