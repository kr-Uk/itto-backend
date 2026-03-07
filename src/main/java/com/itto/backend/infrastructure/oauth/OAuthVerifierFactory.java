package com.itto.backend.infrastructure.oauth;

import com.itto.backend.infrastructure.oauth.OAuthProperties;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
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
