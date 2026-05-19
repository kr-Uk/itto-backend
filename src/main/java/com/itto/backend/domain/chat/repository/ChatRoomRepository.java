package com.itto.backend.domain.chat.repository;

import com.itto.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRelationshipId(Long relationshipId);

    /** 사용자가 참여한 활성 채팅방 목록 (최근 메시지 시각 내림차순, null은 뒤로) */
    @Query("""
            SELECT cr FROM ChatRoom cr
             WHERE cr.isActive = true
               AND (cr.user1.userId = :userId OR cr.user2.userId = :userId)
             ORDER BY CASE WHEN cr.lastMessageAt IS NULL THEN 1 ELSE 0 END,
                      cr.lastMessageAt DESC,
                      cr.id DESC
            """)
    List<ChatRoom> findActiveRoomsByParticipant(@Param("userId") Long userId);
}
