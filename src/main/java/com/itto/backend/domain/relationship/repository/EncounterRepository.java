package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    /**
     * Find all encounters for a relationship, ordered by encounter number
     */
    List<Encounter> findByRelationshipIdOrderByEncounterNumberAsc(Long relationshipId);

    /**
     * Find specific encounter by relationship and encounter number
     */
    Optional<Encounter> findByRelationshipIdAndEncounterNumber(Long relationshipId, int encounterNumber);
}
