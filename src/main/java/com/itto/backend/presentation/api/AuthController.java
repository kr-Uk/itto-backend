package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.auth.*;
import com.itto.backend.application.auth.AuthService;
import com.itto.backend.application.auth.LoginAttemptService;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    @Operation(summary = "소셜 로그인", description = "Firebase ID 토큰으로 로그인/회원가입 처리")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);

        if (loginAttemptService.isBlocked(ip)) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }

        try {
            LoginResponse response = authService.login(request);
            loginAttemptService.resetAttempts(ip);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.OAUTH_VERIFICATION_FAILED) {
                loginAttemptService.recordFailure(ip);
            }
            throw e;
        }
    }

    @Operation(summary = "토큰 갱신", description = "Refresh 토큰으로 새 토큰 발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "로그아웃", description = "현재 세션의 토큰 무효화")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다"));
    }

    @Operation(summary = "회원탈퇴", description = "소프트 딜리트 처리")
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            Authentication authentication,
            @RequestBody(required = false) WithdrawRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        authService.withdraw(userId, request != null ? request : new WithdrawRequest());
        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
