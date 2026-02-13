package com.itto.backend.global.exception;

import com.itto.backend.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [전역 예외 처리기 - 모든 에러를 한 곳에서 처리하는 "에러 관제탑"]
 *
 * 왜 필요한가?
 * - 서비스 곳곳에서 발생하는 에러를 일일이 처리하면 코드가 복잡해집니다.
 * - 이 클래스가 모든 에러를 한 곳에서 잡아서, 프론트엔드에 통일된 형식의 에러 응답을 보내줍니다.
 *
 * 처리하는 에러 종류:
 * 1. BusinessException - 우리가 의도적으로 던진 비즈니스 에러 (예: 사용자 없음, 닉네임 중복)
 * 2. MethodArgumentNotValidException - 입력값 검증 실패 (예: 닉네임이 너무 짧음)
 * 3. Exception - 예상치 못한 모든 에러 (서버 내부 오류)
 */
@Slf4j                  // Lombok: 로그 출력 기능 자동 제공
@RestControllerAdvice   // Spring: 모든 컨트롤러에서 발생하는 예외를 이 클래스가 가로채서 처리하겠다는 선언
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리 (우리가 의도적으로 던진 에러)
     * 예: throw new BusinessException(ErrorCode.USER_NOT_FOUND) → 404 응답
     */
    @ExceptionHandler(BusinessException.class) // Spring: BusinessException이 발생하면 이 메서드가 처리
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());

        // ErrorCode에 정의된 상태 코드와 메시지를 그대로 응답으로 반환
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus(), e.getMessage(), errorCode.getCode()));
    }

    /**
     * 입력값 검증 실패 처리 (예: @Valid로 검증한 요청 데이터가 조건에 맞지 않을 때)
     * 예: 닉네임 필드에 빈 문자열이 들어온 경우 → 400 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // Spring: 입력값 검증 에러 발생 시 이 메서드가 처리
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        // 여러 검증 에러 중 첫 번째 에러 메시지만 꺼냄
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("잘못된 요청입니다");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, message, "BAD_REQUEST"));
    }

    /**
     * 예상치 못한 에러 처리 (위의 두 가지에 해당하지 않는 모든 에러)
     * 예: 데이터베이스 연결 실패, null 참조 에러 등 → 500 응답
     */
    @ExceptionHandler(Exception.class) // Spring: 위에서 잡히지 않은 모든 에러의 마지막 안전망
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);  // 예상치 못한 에러이므로 상세 로그 기록 (디버깅용)

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(500, "서버 내부 오류입니다", "INTERNAL_ERROR"));
    }
}
