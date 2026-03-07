package com.itto.backend.presentation.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * [JWT 토큰 생성 및 검증 담당]
 *
 * JWT(JSON Web Token)란?
 * - 사용자가 로그인하면 서버가 발급해주는 "디지털 출입증" 같은 것입니다.
 * - 앱은 이 토큰을 저장해두고, API를 호출할 때마다 "나 로그인한 사람이야"를 증명하기 위해 함께 보냅니다.
 * - 서버는 이 토큰을 확인해서 누구인지 알아내고, 위조되지 않았는지 검증합니다.
 *
 * 토큰의 종류:
 * - Access Token: 실제 API 호출에 사용하는 토큰 (유효기간 짧음, 예: 30분)
 * - Refresh Token: Access Token이 만료되었을 때 새로 발급받기 위한 토큰 (유효기간 김, 예: 7일)
 */
@Slf4j                   // Lombok: log.warn(), log.error() 같은 로그 출력 기능을 자동으로 사용 가능하게 해줌
@Component               // Spring: 이 클래스를 스프링이 관리하는 객체(Bean)로 등록 → 다른 클래스에서 주입받아 사용 가능
@RequiredArgsConstructor // Lombok: final 필드(jwtProperties)를 매개변수로 받는 생성자를 자동 생성
public class JwtProvider {

    // JWT 관련 설정값 (비밀키, 토큰 만료시간 등)을 담고 있는 객체
    private final JwtProperties jwtProperties;

    /** 비밀키를 가져오는 내부 메서드 (토큰 서명/검증에 사용되는 암호키) */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** Access Token 생성 - 사용자 ID와 역할(role)을 토큰 안에 담아서 발급 */
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiry());

        return Jwts.builder()
                .subject(String.valueOf(userId))   // 토큰의 주인 = 사용자 ID
                .claim("role", role)               // 추가 정보: 사용자 역할 (예: USER, ADMIN)
                .issuedAt(now)                     // 토큰 발급 시각
                .expiration(expiry)                // 토큰 만료 시각
                .signWith(getSigningKey())         // 비밀키로 서명 (위조 방지)
                .compact();                        // 최종 토큰 문자열 생성
    }

    /** Refresh Token 생성 - Access Token 재발급용 (역할 정보 없이 사용자 ID만 포함) */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiry());

        return Jwts.builder()
                .subject(String.valueOf(userId))   // 토큰의 주인 = 사용자 ID
                .claim("type", "refresh")          // 이 토큰이 refresh용임을 표시
                .issuedAt(now)                     // 토큰 발급 시각
                .expiration(expiry)                // 토큰 만료 시각
                .signWith(getSigningKey())         // 비밀키로 서명
                .compact();
    }

    /** 토큰에서 사용자 ID를 꺼내는 메서드 (이 토큰의 주인이 누구인지 확인) */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);          // 토큰을 해독(파싱)
        return Long.parseLong(claims.getSubject());  // subject에 저장된 사용자 ID를 꺼냄
    }

    /** 토큰이 유효한지 검증하는 메서드 (만료 여부, 위조 여부 확인) */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);  // 파싱이 성공하면 유효한 토큰
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 토큰 만료: {}", e.getMessage());       // 유효기간이 지난 토큰
        } catch (JwtException e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());  // 위조되었거나 형식이 잘못된 토큰
        }
        return false;
    }

    /** 토큰을 해독(파싱)하여 내부 정보(Claims)를 꺼내는 내부 메서드 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())    // 비밀키로 서명 검증
                .build()
                .parseSignedClaims(token)       // 토큰을 해독
                .getPayload();                  // 내부 데이터(Claims) 반환
    }
}
