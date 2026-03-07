package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.identity.IdentityRequestResponse;
import com.itto.backend.presentation.dto.identity.IdentityStatusResponse;
import com.itto.backend.application.identity.IdentityVerificationService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Identity Verification", description = "본인인증 API")
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityVerificationService identityVerificationService;

    @Operation(summary = "본인인증 요청", description = "NICE 본인인증 WebView URL을 생성합니다")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<IdentityRequestResponse>> requestVerification(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        IdentityRequestResponse response = identityVerificationService.requestVerification(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "본인인증 성공 콜백", description = "NICE에서 인증 성공 시 호출하는 콜백")
    @PostMapping(value = "/callback/success", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackSuccess(
            @RequestParam("token_version_id") String tokenVersionId,
            @RequestParam("enc_data") String encData,
            @RequestParam("integrity_value") String integrityValue) {
        String html = identityVerificationService.handleSuccessCallback(tokenVersionId, encData, integrityValue);
        return ResponseEntity.ok(html);
    }

    @Operation(summary = "본인인증 실패 콜백", description = "NICE에서 인증 실패 시 호출하는 콜백")
    @PostMapping(value = "/callback/fail", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackFail(
            @RequestParam(value = "token_version_id", required = false) String tokenVersionId,
            @RequestParam(value = "enc_data", required = false) String encData) {
        String html = identityVerificationService.handleFailCallback(tokenVersionId, encData);
        return ResponseEntity.ok(html);
    }

    @Operation(summary = "본인인증 상태 조회", description = "가장 최근 본인인증 결과를 조회합니다")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<IdentityStatusResponse>> getStatus(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        IdentityStatusResponse response = identityVerificationService.getVerificationStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
