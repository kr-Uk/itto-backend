package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.global.enums.RelationshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    /**
     * Find relationship between two users (user_id_1 < user_id_2 guaranteed)
     */
    Optional<Relationship> findByUser1UserIdAndUser2UserId(Long userId1, Long userId2);

    /**
     * Find all relationships for a user (either side)
     */
    @Query("SELECT r FROM Relationship r WHERE r.user1.userId = :userId OR r.user2.userId = :userId")
    List<Relationship> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find by status
     */
    @Query("SELECT r FROM Relationship r WHERE (r.user1.userId = :userId OR r.user2.userId = :userId) AND r.status = :status")
    List<Relationship> findAllByUserIdAndStatus(@Param("userId") Long userId, @Param("status") RelationshipStatus status);

    /**
     * Find expired relationships
     */
    List<Relationship> findByStatusAndExpiresAtBefore(RelationshipStatus status, LocalDateTime now);
}
