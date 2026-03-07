package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.notification.FcmTokenRequest;
import com.itto.backend.presentation.dto.notification.NotificationPageResponse;
import com.itto.backend.presentation.dto.notification.NotificationReadRequest;
import com.itto.backend.application.notification.NotificationService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "페이지네이션을 지원하는 알림 목록 조회")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) authentication.getPrincipal();
        NotificationPageResponse response = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 읽음 처리", description = "선택한 알림들을 읽음 상태로 변경")
    @PutMapping("/notifications/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @Valid @RequestBody NotificationReadRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(userId, request);
        return ResponseEntity.ok(ApiResponse.success("알림이 읽음 처리되었습니다"));
    }

    @Operation(summary = "알림 전체 삭제", description = "사용자의 모든 알림을 삭제")
    @DeleteMapping("/notifications")
    public ResponseEntity<ApiResponse<Void>> deleteAllNotifications(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.deleteAll(userId);
        return ResponseEntity.ok(ApiResponse.success("알림이 전체 삭제되었습니다"));
    }

    @Operation(summary = "FCM 토큰 등록/갱신", description = "푸시 알림을 위한 FCM 토큰 등록 또는 갱신")
    @PutMapping("/fcm/token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            Authentication authentication,
            @Valid @RequestBody FcmTokenRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        notificationService.registerFcmToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success("FCM 토큰이 등록되었습니다"));
    }
}
