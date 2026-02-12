package com.itto.backend.domain.block.repository;

import com.itto.backend.domain.block.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
