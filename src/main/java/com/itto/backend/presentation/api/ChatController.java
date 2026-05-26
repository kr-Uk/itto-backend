package com.itto.backend.presentation.api;

import com.itto.backend.application.chat.ChatService;
import com.itto.backend.presentation.dto.chat.ChatImageUploadResponse;
import com.itto.backend.presentation.dto.chat.ChatMessagePageResponse;
import com.itto.backend.presentation.dto.chat.ChatReadResponse;
import com.itto.backend.presentation.dto.chat.ChatRoomListResponse;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Chat", description = "채팅 REST API (실시간 송수신은 STOMP /ws)")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "내 채팅방 목록", description = "내가 참여한 활성 채팅방 목록을 마지막 메시지 시각 내림차순으로 반환합니다")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomListResponse>> listRooms(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(chatService.listRooms(userId)));
    }

    @Operation(summary = "채팅방 메시지 페이지네이션", description = "최신 메시지부터 페이지 단위로 조회합니다")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getMessages(userId, chatRoomId, page, size)
        ));
    }

    @Operation(summary = "채팅방 읽음 처리", description = "상대방이 보낸 미읽음 메시지를 모두 읽음 처리합니다")
    @PostMapping("/rooms/{chatRoomId}/read")
    public ResponseEntity<ApiResponse<ChatReadResponse>> markAsRead(
            Authentication authentication,
            @PathVariable Long chatRoomId) {
        Long userId = (Long) authentication.getPrincipal();
        int updated = chatService.markAsRead(userId, chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(ChatReadResponse.builder().updatedCount(updated).build()));
    }

    @Operation(summary = "채팅 이미지 업로드", description = "MinIO에 이미지를 업로드하고 URL을 반환합니다. 반환된 URL은 STOMP send 시 imageUrl로 전달합니다")
    @PostMapping(value = "/rooms/{chatRoomId}/images", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ChatImageUploadResponse>> uploadImage(
            Authentication authentication,
            @PathVariable Long chatRoomId,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                chatService.uploadImage(userId, chatRoomId, file)
        ));
    }
}
