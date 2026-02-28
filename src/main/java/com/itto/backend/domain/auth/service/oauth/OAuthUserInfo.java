package com.itto.backend.domain.auth.service.oauth;

public record OAuthUserInfo(
        String providerUid,
        String provider,
        String email
) {
}
