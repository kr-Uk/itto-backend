package com.itto.backend.domain.admin.controller;
import com.itto.backend.presentation.api.AdminController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itto.backend.presentation.dto.admin.RejectRequest;
import com.itto.backend.presentation.dto.admin.VerificationListResponse;
import com.itto.backend.application.admin.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Test
    @DisplayName("승인 대기 목록 조회 성공")
    @WithMockUser
    void getPendingVerifications_Success() throws Exception {
        // given
        VerificationListResponse.VerificationInfo info = VerificationListResponse.VerificationInfo.builder()
                .id(1L)
                .userId(1L)
                .nickname("유저1")
                .profilePhotos(List.of("https://example.com/photo1.jpg"))
                .selfieUrl(null)
                .requestedAt(LocalDateTime.now())
                .build();

        VerificationListResponse response = VerificationListResponse.builder()
                .content(List.of(info))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        given(adminService.getPendingVerifications(0, 20)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/admin/verifications")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].userId").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("인증 승인 성공")
    @WithMockUser
    void approveVerification_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/admin/verifications/1/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("승인되었습니다"));

        verify(adminService).approveVerification(1L);
    }

    @Test
    @DisplayName("인증 거절 성공")
    @WithMockUser
    void rejectVerification_Success() throws Exception {
        // given
        RejectRequest request = new RejectRequest();
        // RejectRequest는 @NoArgsConstructor만 있어서 직접 필드 설정 불가
        // Reflection을 사용하거나 JSON으로 직접 전달
        String requestJson = """
                {
                    "reason": "얼굴이 잘 보이지 않습니다"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/admin/verifications/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("거절되었습니다"));

        verify(adminService).rejectVerification(eq(1L), any(RejectRequest.class));
    }

    @Test
    @DisplayName("인증 거절 실패 - 거절 사유 누락")
    @WithMockUser
    void rejectVerification_Fail_BlankReason() throws Exception {
        // given
        String requestJson = """
                {
                    "reason": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/admin/verifications/1/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}
