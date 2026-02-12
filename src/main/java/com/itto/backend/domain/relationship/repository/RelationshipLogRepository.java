package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.RelationshipLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationshipLogRepository extends JpaRepository<RelationshipLog, Long> {

    /**
     * Find all logs for a relationship, ordered by creation time (most recent first)
     */
    List<RelationshipLog> findByRelationshipIdOrderByCreatedAtDesc(Long relationshipId);
}
