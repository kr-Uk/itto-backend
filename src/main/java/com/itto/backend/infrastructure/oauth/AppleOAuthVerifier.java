package com.itto.backend.infrastructure.oauth;

import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

@Slf4j
public class AppleOAuthVerifier implements OAuthVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final String clientId;

    public AppleOAuthVerifier(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public OAuthUserInfo verify(String token) {
        try {
            JWKSet jwkSet = JWKSet.load(new URL(APPLE_JWKS_URL));

            DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, new ImmutableJWKSet<>(jwkSet)));

            jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    new JWTClaimsSet.Builder()
                            .issuer(APPLE_ISSUER)
                            .audience(clientId)
                            .build(),
                    new HashSet<>(Arrays.asList("sub", "iat", "exp"))
            ));

            JWTClaimsSet claims = jwtProcessor.process(token, null);

            return new OAuthUserInfo(
                    claims.getSubject(),
                    "APPLE",
                    claims.getStringClaim("email")
            );
        } catch (Exception e) {
            log.warn("Apple OAuth 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }
}
