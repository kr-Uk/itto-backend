package com.itto.backend.application.chat;

import com.itto.backend.domain.chat.entity.ChatMessage;
import com.itto.backend.domain.chat.entity.ChatRoom;
import com.itto.backend.domain.chat.repository.ChatMessageRepository;
import com.itto.backend.domain.chat.repository.ChatRoomRepository;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.infrastructure.minio.StorageService;
import com.itto.backend.presentation.dto.chat.ChatImageUploadResponse;
import com.itto.backend.presentation.dto.chat.ChatMessagePageResponse;
import com.itto.backend.presentation.dto.chat.ChatMessageResponse;
import com.itto.backend.presentation.dto.chat.ChatRoomListResponse;
import com.itto.backend.presentation.dto.chat.ChatRoomSummary;
import com.itto.backend.presentation.dto.chat.ChatSendRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    /** 채팅방 마지막 메시지 미리보기 최대 길이 */
    private static final int LAST_MESSAGE_PREVIEW_LIMIT = 100;
    private static final String IMAGE_PLACEHOLDER = "[사진]";
    private static final String CHAT_IMAGE_DIRECTORY = "chat-images";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;

    // --- 채팅방 목록 ---
    public ChatRoomListResponse listRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findActiveRoomsByParticipant(userId);

        List<ChatRoomSummary> summaries = rooms.stream()
                .map(room -> {
                    User other = room.getOtherUser(userId);
                    String photo = userPhotoRepository.findByUserUserIdAndIsRepresentativeTrue(other.getUserId())
                            .map(UserPhoto::getPhotoUrl)
                            .orElse(null);
                    long unread = chatMessageRepository
                            .countByChatRoomIdAndSenderUserIdNotAndIsReadFalse(room.getId(), userId);

                    return ChatRoomSummary.builder()
                            .chatRoomId(room.getId())
                            .relationshipId(room.getRelationship().getId())
                            .otherUserId(other.getUserId())
                            .otherUserNickname(other.getNickname())
                            .otherUserPhotoUrl(photo)
                            .lastMessage(room.getLastMessage())
                            .lastMessageAt(room.getLastMessageAt())
                            .unreadCount(unread)
                            .expiresAt(room.getExpiresAt())
                            .build();
                })
                .toList();

        return ChatRoomListResponse.builder().rooms(summaries).build();
    }

    // --- 메시지 페이지네이션 ---
    public ChatMessagePageResponse getMessages(Long userId, Long chatRoomId, int page, int size) {
        ChatRoom room = getRoomAndValidate(chatRoomId, userId);

        Page<ChatMessage> messagePage = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(page, size));

        List<ChatMessageResponse> content = messagePage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return ChatMessagePageResponse.builder()
                .content(content)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .build();
    }

    // --- 메시지 전송 (STOMP/REST 공통) ---
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, ChatSendRequest request) {
        ChatRoom room = getRoomAndValidate(request.getChatRoomId(), userId);
        if (!Boolean.TRUE.equals(room.getIsActive())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INACTIVE);
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isBlank();
        if (!hasContent && !hasImage) {
            throw new BusinessException(ErrorCode.EMPTY_MESSAGE);
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(hasContent ? request.getContent() : null)
                .imageUrl(hasImage ? request.getImageUrl() : null)
                .isRead(false)
                .build());

        String preview = hasContent ? truncate(request.getContent()) : IMAGE_PLACEHOLDER;
        room.updateLastMessage(preview, LocalDateTime.now());

        ChatMessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/topic/chat-rooms/" + room.getId(), response);
        return response;
    }

    // --- 읽음 처리 ---
    @Transactional
    public int markAsRead(Long userId, Long chatRoomId) {
        ChatRoom room = getRoomAndValidate(chatRoomId, userId);
        return chatMessageRepository.markAllAsReadByReceiver(room.getId(), userId);
    }

    // --- 이미지 업로드 (MinIO) ---
    public ChatImageUploadResponse uploadImage(Long userId, Long chatRoomId, MultipartFile file) {
        ChatRoom room = getRoomAndValidate(chatRoomId, userId);
        String url = storageService.uploadFile(file, CHAT_IMAGE_DIRECTORY + "/" + room.getId());
        return ChatImageUploadResponse.builder().imageUrl(url).build();
    }

    // --- 채팅방 자동 생성 (RelationshipService에서 호출) ---
    @Transactional
    public ChatRoom createForRelationship(Relationship relationship, LocalDateTime expiresAt) {
        return chatRoomRepository.save(ChatRoom.builder()
                .relationship(relationship)
                .user1(relationship.getUser1())
                .user2(relationship.getUser2())
                .isActive(true)
                .expiresAt(expiresAt)
                .build());
    }

    // --- helpers ---

    private ChatRoom getRoomAndValidate(Long chatRoomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!room.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.NOT_CHAT_PARTICIPANT);
        }
        return room;
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getUserId())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String truncate(String text) {
        if (text.length() <= LAST_MESSAGE_PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, LAST_MESSAGE_PREVIEW_LIMIT);
    }
}
