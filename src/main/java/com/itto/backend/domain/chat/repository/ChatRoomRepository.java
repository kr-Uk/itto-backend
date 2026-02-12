package com.itto.backend.domain.chat.repository;

import com.itto.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find chat room by relationship ID
     */
    Optional<ChatRoom> findByRelationshipId(Long relationshipId);

    /**
     * Find chat room by Firestore room ID
     */
    Optional<ChatRoom> findByFirestoreRoomId(String firestoreRoomId);
}
