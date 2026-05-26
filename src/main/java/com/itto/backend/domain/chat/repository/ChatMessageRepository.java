package com.itto.backend.domain.chat.repository;

import com.itto.backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 채팅방 메시지 페이지네이션 (최신순) */
    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    /** 상대방이 보낸 미읽음 메시지를 모두 읽음 처리 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatMessage m
               SET m.isRead = true
             WHERE m.chatRoom.id = :chatRoomId
               AND m.sender.userId <> :readerUserId
               AND m.isRead = false
            """)
    int markAllAsReadByReceiver(@Param("chatRoomId") Long chatRoomId,
                                @Param("readerUserId") Long readerUserId);

    /** 채팅방별 상대방이 보낸 미읽음 메시지 수 */
    long countByChatRoomIdAndSenderUserIdNotAndIsReadFalse(Long chatRoomId, Long readerUserId);
}
