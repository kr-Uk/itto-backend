package com.itto.backend.infrastructure.oauth;

public interface OAuthVerifier {
    OAuthUserInfo verify(String token);
}
