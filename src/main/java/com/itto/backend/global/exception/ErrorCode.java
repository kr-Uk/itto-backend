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
    USER_ALREADY_WITHDRAWN(400, "이미 탈퇴한 사용자입니다", "USER_ALREADY_WITHDRAWN"),

    // 회원가입
    NICKNAME_TOO_SHORT(400, "닉네임은 2자 이상이어야 합니다", "NICKNAME_TOO_SHORT"),
    NICKNAME_TOO_LONG(400, "닉네임은 8자 이하여야 합니다", "NICKNAME_TOO_LONG"),
    NICKNAME_PROFANITY(400, "부적절한 단어가 포함되어 있습니다", "NICKNAME_PROFANITY"),
    NICKNAME_DUPLICATE(409, "이미 사용 중인 닉네임입니다", "NICKNAME_DUPLICATE"),
    AGE_RESTRICTION(400, "만 19세 미만은 가입할 수 없습니다", "AGE_RESTRICTION"),
    HOBBY_MIN(400, "취미는 최소 1개를 선택해야 합니다", "HOBBY_MIN"),
    HOBBY_MAX(400, "취미는 최대 3개까지 선택할 수 있습니다", "HOBBY_MAX"),
    HOBBY_NOT_FOUND(404, "존재하지 않는 취미입니다", "HOBBY_NOT_FOUND"),
    PHOTO_MIN(400, "사진은 최소 1장 필요합니다", "PHOTO_MIN"),
    PHOTO_MAX(400, "사진은 최대 3장까지 업로드할 수 있습니다", "PHOTO_MAX"),
    PROFILE_INCOMPLETE(400, "프로필 정보를 먼저 입력해주세요", "PROFILE_INCOMPLETE"),
    TERMS_NOT_AGREED(400, "필수 약관에 동의해주세요", "TERMS_NOT_AGREED"),
    FILE_UPLOAD_FAILED(500, "파일 업로드에 실패했습니다", "FILE_UPLOAD_FAILED"),

    // 프로필
    MIN_PHOTO_REQUIRED(400, "사진은 최소 1장 필요합니다", "MIN_PHOTO_REQUIRED"),
    PHOTO_NOT_FOUND(404, "사진을 찾을 수 없습니다", "PHOTO_NOT_FOUND"),
    RELATIONSHIP_NOT_FOUND(404, "관계를 찾을 수 없습니다", "RELATIONSHIP_NOT_FOUND");

    private final int status;
    private final String message;
    private final String code;
}
