package com.itto.backend.infrastructure.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class GoogleOAuthVerifier implements OAuthVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthVerifier(String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public OAuthUserInfo verify(String token) {
        try {
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            return new OAuthUserInfo(
                    payload.getSubject(),
                    "GOOGLE",
                    payload.getEmail()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google OAuth 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }
}
