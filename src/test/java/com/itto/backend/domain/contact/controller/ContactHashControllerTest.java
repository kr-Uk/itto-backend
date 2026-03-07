package com.itto.backend.domain.contact.controller;
import com.itto.backend.presentation.api.ContactHashController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itto.backend.presentation.dto.contact.ContactHashUploadRequest;
import com.itto.backend.presentation.dto.contact.ManualBlockRequest;
import com.itto.backend.domain.contact.entity.ContactHash;
import com.itto.backend.application.contact.ContactHashService;
import com.itto.backend.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactHashController.class)
@DisplayName("ContactHashController")
class ContactHashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContactHashService contactHashService;

    private void setAuthenticatedUser(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /api/v1/contacts/hashes")
    class UploadHashesTest {

        @Test
        @DisplayName("해시 업로드 성공 → 201")
        @WithMockUser
        void uploadSuccess() throws Exception {
            setAuthenticatedUser(1L);
            ContactHashUploadRequest request = ContactHashUploadRequest.builder()
                    .hashes(List.of("a".repeat(64), "b".repeat(64)))
                    .build();

            mockMvc.perform(post("/api/v1/contacts/hashes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("연락처 해시가 동기화되었습니다"))
                    .andExpect(jsonPath("$.data.uploadedCount").value(2));
        }

        @Test
        @DisplayName("빈 body → 400")
        @WithMockUser
        void uploadEmptyBody() throws Exception {
            setAuthenticatedUser(1L);

            mockMvc.perform(post("/api/v1/contacts/hashes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없음 → 401")
        void uploadNoAuth() throws Exception {
            ContactHashUploadRequest request = ContactHashUploadRequest.builder()
                    .hashes(List.of("a".repeat(64)))
                    .build();

            mockMvc.perform(post("/api/v1/contacts/hashes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/contacts/hashes")
    class GetHashesTest {

        @Test
        @DisplayName("해시 목록 조회 → 200")
        @WithMockUser
        void getHashesSuccess() throws Exception {
            setAuthenticatedUser(1L);

            given(contactHashService.getHashes(1L)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/contacts/hashes")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hashes").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/contacts/block-by-phone")
    class BlockByPhoneTest {

        @Test
        @DisplayName("직접 번호 입력 차단 → 201")
        @WithMockUser
        void blockByPhoneSuccess() throws Exception {
            setAuthenticatedUser(1L);
            ManualBlockRequest request = ManualBlockRequest.builder()
                    .phoneNumber("010-1234-5678")
                    .build();

            mockMvc.perform(post("/api/v1/contacts/block-by-phone")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("번호가 차단 목록에 추가되었습니다"));
        }

        @Test
        @DisplayName("요청에 원문 전화번호가 있지만 응답에는 없음 (보안)")
        @WithMockUser
        void responseDoesNotContainPhoneNumber() throws Exception {
            setAuthenticatedUser(1L);
            ManualBlockRequest request = ManualBlockRequest.builder()
                    .phoneNumber("010-9999-8888")
                    .build();

            String responseBody = mockMvc.perform(post("/api/v1/contacts/block-by-phone")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            org.assertj.core.api.Assertions.assertThat(responseBody)
                    .doesNotContain("010-9999-8888")
                    .doesNotContain("01099998888");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/contacts/hashes/{hashId}")
    class RemoveHashTest {

        @Test
        @DisplayName("단건 삭제 → 200")
        @WithMockUser
        void removeSuccess() throws Exception {
            setAuthenticatedUser(1L);

            mockMvc.perform(delete("/api/v1/contacts/hashes/10")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("차단이 해제되었습니다"));
        }
    }
}
