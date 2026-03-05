package com.itto.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [에러 코드 모음 - 서비스에서 발생할 수 있는 모든 에러를 한 곳에 정의]
 *
 * enum(열거형)이란?
 * - 미리 정해둔 값들의 목록입니다. 여기서는 "에러의 종류"를 목록으로 관리합니다.
 * - 예: USER_NOT_FOUND, NICKNAME_DUPLICATE 등
 *
 * 각 에러 코드는 3가지 정보를 가집니다:
 * - status: HTTP 상태 코드 (400=잘못된 요청, 401=인증 필요, 404=못 찾음, 500=서버 오류)
 * - message: 사용자에게 보여줄 한국어 에러 메시지
 * - code: 프론트엔드가 에러를 구분하기 위한 영문 코드 (예: "USER_NOT_FOUND")
 *
 * 사용 예시: throw new BusinessException(ErrorCode.USER_NOT_FOUND)
 * → 자동으로 404 상태 코드 + "사용자를 찾을 수 없습니다" 메시지가 응답됨
 */
@Getter                  // Lombok: getStatus(), getMessage(), getCode() 메서드를 자동 생성
@RequiredArgsConstructor // Lombok: (status, message, code)를 받는 생성자를 자동 생성
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
    RELATIONSHIP_NOT_FOUND(404, "관계를 찾을 수 없습니다", "RELATIONSHIP_NOT_FOUND"),

    // 차단
    ALREADY_BLOCKED(409, "이미 차단한 사용자입니다", "ALREADY_BLOCKED"),
    CANNOT_BLOCK_SELF(400, "자기 자신을 차단할 수 없습니다", "CANNOT_BLOCK_SELF"),
    BLOCK_NOT_FOUND(404, "차단 정보를 찾을 수 없습니다", "BLOCK_NOT_FOUND"),

    // 관계
    NOT_PILYEON(400, "필연 상태가 아닙니다", "NOT_PILYEON"),
    NOT_UYEON(400, "우연 상태가 아닙니다", "NOT_UYEON"),
    NOT_PENDING(400, "대기 상태가 아닙니다", "NOT_PENDING"),
    NOT_PAST(400, "지난인연 상태가 아닙니다", "NOT_PAST"),
    NOT_INYEON(400, "인연 상태가 아닙니다", "NOT_INYEON"),
    ALREADY_PENDING(409, "이미 인연 신청 중입니다", "ALREADY_PENDING"),
    NOT_RELATIONSHIP_MEMBER(403, "해당 관계의 참여자가 아닙니다", "NOT_RELATIONSHIP_MEMBER"),
    INSUFFICIENT_KNOTS(400, "매듭이 부족합니다", "INSUFFICIENT_KNOTS"),
    ENCOUNTER_NOT_FOUND(404, "조우 정보를 찾을 수 없습니다", "ENCOUNTER_NOT_FOUND"),

    // 위치
    EXCLUDED_AREA_NOT_FOUND(404, "제외 지역을 찾을 수 없습니다", "EXCLUDED_AREA_NOT_FOUND"),
    EXCLUDED_AREA_LIMIT(400, "제외 지역은 최대 3개까지 등록할 수 있습니다", "EXCLUDED_AREA_LIMIT"),
    EXCLUDED_CELLS_LIMIT(400, "제외 셀은 최대 20개까지 선택할 수 있습니다", "EXCLUDED_CELLS_LIMIT"),

    // 연락처 해시
    INVALID_HASH_FORMAT(400, "잘못된 해시 형식입니다 (64자 hex string 필요)", "INVALID_HASH_FORMAT"),
    CONTACT_HASH_NOT_FOUND(404, "연락처 해시를 찾을 수 없습니다", "CONTACT_HASH_NOT_FOUND"),
    HASH_LIMIT_EXCEEDED(400, "연락처 해시 업로드 제한을 초과했습니다 (최대 5000개)", "HASH_LIMIT_EXCEEDED"),

    // 결제
    INVALID_RECEIPT(400, "유효하지 않은 영수증입니다", "INVALID_RECEIPT"),
    FREE_USAGE_EXHAUSTED(400, "이번 달 무료 횟수를 모두 사용했습니다", "FREE_USAGE_EXHAUSTED");

    private final int status;    // HTTP 상태 코드 (예: 400, 401, 404, 500)
    private final String message; // 사용자에게 보여줄 에러 메시지
    private final String code;    // 프론트엔드가 에러를 구분하기 위한 영문 코드
}
