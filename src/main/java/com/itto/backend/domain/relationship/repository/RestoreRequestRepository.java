package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.RestoreRequest;
import com.itto.backend.global.enums.RestoreRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestoreRequestRepository extends JpaRepository<RestoreRequest, Long> {

    /**
     * Find all restore requests received by user with specific status
     */
    List<RestoreRequest> findByReceiverUserIdAndStatus(Long receiverId, RestoreRequestStatus status);

    /**
     * Find restore request by relationship and status
     */
    Optional<RestoreRequest> findByRelationshipIdAndStatus(Long relationshipId, RestoreRequestStatus status);
}
