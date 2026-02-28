package com.itto.backend.domain.auth.service.oauth;

public interface OAuthVerifier {
    OAuthUserInfo verify(String token);
}
