package com.itto.backend.presentation.exception;

import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.presentation.dto.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleBusinessException")
    class HandleBusinessExceptionTest {

        @Test
        @DisplayName("USER_NOT_FOUND → 404 응답")
        void userNotFound() {
            // given
            BusinessException ex = new BusinessException(ErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo("사용자를 찾을 수 없습니다");
            assertThat(response.getBody().getCode()).isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("INVALID_REFRESH_TOKEN → 401 응답")
        void invalidRefreshToken() {
            // given
            BusinessException ex = new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INVALID_REFRESH_TOKEN");
        }

        @Test
        @DisplayName("ALREADY_BLOCKED → 409 응답")
        void alreadyBlocked() {
            // given
            BusinessException ex = new BusinessException(ErrorCode.ALREADY_BLOCKED);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("ALREADY_BLOCKED");
        }

        @Test
        @DisplayName("커스텀 메시지가 있는 BusinessException")
        void customMessage() {
            // given
            BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST, "커스텀 에러 메시지");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("커스텀 에러 메시지");
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationExceptionTest {

        @Test
        @DisplayName("유효성 검증 실패 → 400 응답 + 첫 번째 필드 에러 메시지")
        void validationError() throws Exception {
            // given
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new Object(), "request");
            bindingResult.addError(new FieldError(
                    "request", "nickname", "닉네임은 2자 이상이어야 합니다"));

            MethodParameter methodParameter = new MethodParameter(
                    this.getClass().getDeclaredMethod("validationError"), -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                    methodParameter, bindingResult);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("닉네임은 2자 이상이어야 합니다");
            assertThat(response.getBody().getCode()).isEqualTo("BAD_REQUEST");
        }

        @Test
        @DisplayName("필드 에러가 없는 경우 기본 메시지")
        void validationErrorNoFieldError() throws Exception {
            // given
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new Object(), "request");

            MethodParameter methodParameter = new MethodParameter(
                    this.getClass().getDeclaredMethod("validationErrorNoFieldError"), -1);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                    methodParameter, bindingResult);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다");
        }
    }

    @Nested
    @DisplayName("handleException")
    class HandleExceptionTest {

        @Test
        @DisplayName("일반 Exception → 500 응답")
        void generalException() {
            // given
            Exception ex = new RuntimeException("unexpected error");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo("서버 내부 오류입니다");
            assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("NullPointerException → 500 응답")
        void nullPointerException() {
            // given
            Exception ex = new NullPointerException("null reference");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_ERROR");
        }
    }
}
