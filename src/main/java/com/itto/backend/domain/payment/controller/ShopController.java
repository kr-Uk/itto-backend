package com.itto.backend.domain.payment.controller;

import com.itto.backend.domain.payment.dto.*;
import com.itto.backend.domain.payment.service.ShopService;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Tag(name = "Shop", description = "상점/결제 API")
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/knots")
    @Operation(summary = "매듭 잔액 조회", description = "현재 매듭 잔액과 무료 횟수를 조회합니다")
    public ResponseEntity<ApiResponse<KnotsBalanceResponse>> getKnotsBalance(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        KnotsBalanceResponse response = shopService.getKnotsBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/packages")
    @Operation(summary = "매듭 패키지 목록 조회", description = "구매 가능한 매듭 패키지 목록을 조회합니다")
    public ResponseEntity<ApiResponse<PackageListResponse>> getPackages(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        PackageListResponse response = shopService.getPackages(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/purchase")
    @Operation(summary = "매듭 구매", description = "매듭 패키지를 구매합니다")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchase(
            Authentication authentication,
            @Valid @RequestBody PurchaseRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        PurchaseResponse response = shopService.purchase(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("구매가 완료되었습니다", response));
    }

    @GetMapping("/subscription")
    @Operation(summary = "구독 상태 조회", description = "현재 구독 상태와 혜택을 조회합니다")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SubscriptionResponse response = shopService.getSubscription(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/subscription")
    @Operation(summary = "구독 시작", description = "프리미엄 구독을 시작합니다")
    public ResponseEntity<ApiResponse<SubscriptionStartResponse>> startSubscription(
            Authentication authentication,
            @Valid @RequestBody SubscriptionStartRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        SubscriptionStartResponse response = shopService.startSubscription(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("프리미엄 구독이 시작되었습니다", response));
    }

    @DeleteMapping("/subscription")
    @Operation(summary = "구독 해지", description = "프리미엄 구독을 해지합니다")
    public ResponseEntity<ApiResponse<SubscriptionCancelResponse>> cancelSubscription(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SubscriptionCancelResponse response = shopService.cancelSubscription(userId);
        return ResponseEntity.ok(ApiResponse.success("구독이 해지되었습니다", response));
    }
}
