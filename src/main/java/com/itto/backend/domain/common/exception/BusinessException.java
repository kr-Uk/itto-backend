package com.itto.backend.domain.common.exception;

import lombok.Getter;

/**
 * [비즈니스 로직 전용 예외 클래스]
 *
 * 예외(Exception)란?
 * - 프로그램 실행 중 "예상된 문제 상황"이 발생했을 때 사용하는 에러 알림 장치입니다.
 * - 예: 존재하지 않는 사용자 조회, 이미 사용 중인 닉네임 등록 시도 등
 *
 * 왜 필요한가?
 * - 서비스 로직에서 문제가 생기면 이 예외를 던지고(throw),
 *   GlobalExceptionHandler가 잡아서(catch) 적절한 에러 응답으로 변환해줍니다.
 * - ErrorCode와 함께 사용하여 에러의 종류와 메시지를 통일되게 관리합니다.
 *
 * 사용 예시:
 *   throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 *   → GlobalExceptionHandler가 잡아서 → { "status": 404, "message": "사용자를 찾을 수 없습니다" } 응답
 *
 * RuntimeException을 상속:
 * - RuntimeException은 자바에서 "실행 중 발생하는 예외"의 부모 클래스입니다.
 * - 이를 상속하면 메서드 선언에 throws를 붙이지 않아도 되어 코드가 깔끔해집니다.
 */
@Getter  // Lombok: getErrorCode() 메서드를 자동 생성
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;  // 어떤 에러인지 구분하는 에러 코드 (예: USER_NOT_FOUND)

    /** 에러 코드만으로 예외 생성 (에러 메시지는 ErrorCode에 정의된 기본 메시지 사용) */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());  // 부모 클래스(RuntimeException)에 에러 메시지 전달
        this.errorCode = errorCode;
    }

    /** 에러 코드 + 커스텀 메시지로 예외 생성 (기본 메시지 대신 직접 지정한 메시지 사용) */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
