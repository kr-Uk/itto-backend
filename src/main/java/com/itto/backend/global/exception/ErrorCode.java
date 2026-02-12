package com.itto.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    BAD_REQUEST(400, "잘못된 요청입니다", "BAD_REQUEST"),
    UNAUTHORIZED(401, "인증이 필요합니다", "UNAUTHORIZED"),
    FORBIDDEN(403, "권한이 없습니다", "FORBIDDEN"),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다", "NOT_FOUND"),
    CONFLICT(409, "중복 또는 상태 충돌입니다", "CONFLICT"),
    INTERNAL_ERROR(500, "서버 내부 오류입니다", "INTERNAL_ERROR"),

    // 인증
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다", "INVALID_TOKEN"),
    EXPIRED_TOKEN(401, "만료된 토큰입니다", "EXPIRED_TOKEN"),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 리프레시 토큰입니다", "INVALID_REFRESH_TOKEN"),
    FIREBASE_AUTH_FAILED(401, "Firebase 인증에 실패했습니다", "FIREBASE_AUTH_FAILED"),

    // 유저
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다", "USER_NOT_FOUND"),
    USER_ALREADY_WITHDRAWN(400, "이미 탈퇴한 사용자입니다", "USER_ALREADY_WITHDRAWN");

    private final int status;
    private final String message;
    private final String code;
}
