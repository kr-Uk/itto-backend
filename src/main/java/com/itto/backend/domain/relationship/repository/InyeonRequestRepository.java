package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.InyeonRequest;
import com.itto.backend.domain.common.enums.InyeonRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InyeonRequestRepository extends JpaRepository<InyeonRequest, Long> {

    /**
     * Find all inyeon requests received by user with specific status
     */
    List<InyeonRequest> findByReceiverUserIdAndStatus(Long receiverId, InyeonRequestStatus status);

    /**
     * Find inyeon request by relationship and status
     */
    Optional<InyeonRequest> findByRelationshipIdAndStatus(Long relationshipId, InyeonRequestStatus status);

    /**
     * Find all inyeon requests sent by user
     */
    List<InyeonRequest> findByRequesterUserId(Long requesterId);
}
