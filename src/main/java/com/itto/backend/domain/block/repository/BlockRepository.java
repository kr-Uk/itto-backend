package com.itto.backend.domain.block.repository;

import com.itto.backend.domain.block.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Find all blocks created by a user
     */
    List<Block> findByBlockerUserId(Long blockerId);

    /**
     * Check if a user has blocked another user
     */
    boolean existsByBlockerUserIdAndBlockedUserId(Long blockerId, Long blockedId);

    /**
     * Find specific block relationship
     */
    Optional<Block> findByBlockerUserIdAndBlockedUserId(Long blockerId, Long blockedId);

    /**
     * Check if there's a block in either direction between two users
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE (b.blocker.userId = :userA AND b.blocked.userId = :userB) " +
           "OR (b.blocker.userId = :userB AND b.blocked.userId = :userA)")
    boolean existsBlockBetween(@Param("userA") Long userA, @Param("userB") Long userB);
}
