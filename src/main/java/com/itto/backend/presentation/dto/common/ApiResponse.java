package com.itto.backend.presentation.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * [API 공통 응답 래퍼]
 *
 * 모든 API 응답을 감싸는 공통 형식입니다.
 * 프론트엔드(앱)에서 항상 같은 형태의 JSON을 받을 수 있도록 통일해줍니다.
 *
 * 성공 예시: { "status": 200, "message": "성공", "data": { ... } }
 * 실패 예시: { "status": 404, "message": "사용자를 찾을 수 없습니다", "code": "USER_NOT_FOUND" }
 *
 * @param <T> data 필드에 들어갈 데이터의 타입 (어떤 타입이든 가능)
 */
@Getter                                      // Lombok: 각 필드의 getter 메서드(getStatus(), getMessage() 등)를 자동 생성
@Builder                                     // Lombok: 빌더 패턴으로 객체를 생성할 수 있게 해줌 (예: ApiResponse.builder().status(200).build())
@AllArgsConstructor                          // Lombok: 모든 필드를 매개변수로 받는 생성자를 자동 생성
@JsonInclude(JsonInclude.Include.NON_NULL)   // Jackson: 값이 null인 필드는 JSON 응답에서 아예 빼줌 (불필요한 정보 제거)
public class ApiResponse<T> {

    private int status;       // HTTP 상태 코드 (200=성공, 400=잘못된 요청, 404=못 찾음, 500=서버 오류 등)
    private String message;   // 사람이 읽을 수 있는 응답 메시지 (예: "성공", "사용자를 찾을 수 없습니다")
    private T data;           // 실제 응답 데이터 (사용자 정보, 목록 등 - 성공 시에만 포함)
    private String code;      // 에러 코드 문자열 (실패 시에만 포함, 예: "USER_NOT_FOUND")

    /** 데이터만 넘기는 성공 응답 (메시지는 기본값 "성공") */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message("성공")
                .data(data)
                .build();
    }

    /** 메시지만 넘기는 성공 응답 (데이터 없이 메시지만 전달) */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .build();
    }

    /** 메시지와 데이터를 함께 넘기는 성공 응답 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    /** 실패(에러) 응답 - 상태 코드, 에러 메시지, 에러 코드를 담아 반환 */
    public static ApiResponse<Void> error(int status, String message, String code) {
        return ApiResponse.<Void>builder()
                .status(status)
                .message(message)
                .code(code)
                .build();
    }
}
