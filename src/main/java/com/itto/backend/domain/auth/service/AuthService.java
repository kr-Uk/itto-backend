package com.itto.backend.domain.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.itto.backend.domain.auth.dto.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.FcmTokenRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import com.itto.backend.global.security.JwtProperties;
import com.itto.backend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Firebase 토큰 검증
        FirebaseToken firebaseToken = verifyFirebaseToken(request.getFirebaseIdToken());
        String firebaseUid = firebaseToken.getUid();

        // 2. 유저 조회 또는 생성
        boolean isNewUser = false;
        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);

        if (user == null) {
            // 신규 유저 생성
            Provider provider = Provider.valueOf(request.getProvider().toUpperCase());
            user = User.builder()
                    .firebaseUid(firebaseUid)
                    .provider(provider)
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
        } else if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 3. 최근 로그인 시간 갱신
        user.updateLastLoginAt();

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

        // 5. Refresh 토큰 Redis 저장
        saveRefreshToken(user.getUserId(), refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(isNewUser)
                .verificationStatus(user.getVerificationStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Refresh 토큰 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. userId 추출
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);

        // 3. Redis에 저장된 토큰과 일치하는지 확인
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 5. 새 토큰 발급
        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        // 6. Redis 갱신
        saveRefreshToken(userId, newRefreshToken);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional(readOnly = true)
    public void logout(Long userId) {
        // Redis에서 Refresh 토큰 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        // 소프트 딜리트
        user.softDelete();

        // Redis에서 Refresh 토큰 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        // FCM 토큰 삭제
        fcmTokenRepository.deleteByUserUserId(userId);

        log.info("회원탈퇴 처리 완료: userId={}, reason={}", userId, request.getReason());
    }

    private FirebaseToken verifyFirebaseToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FIREBASE_AUTH_FAILED);
        }
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiry())
        );
    }
}
