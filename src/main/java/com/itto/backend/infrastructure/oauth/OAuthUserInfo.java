package com.itto.backend.infrastructure.oauth;

public record OAuthUserInfo(
        String providerUid,
        String provider,
        String email
) {
}
