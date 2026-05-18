package com.itto.backend.domain.relationship.repository;

import com.itto.backend.domain.relationship.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 사용자가 참여한 모든 encounter를 h3Index별로 그룹핑하여 카운트.
     * 지도 화면의 H3 헥사곤 heatmap에 사용.
     */
    interface H3CountRow {
        String getH3Index();
        long getCnt();
    }

    @Query("SELECT e.h3Index AS h3Index, COUNT(e) AS cnt FROM Encounter e " +
            "WHERE e.user1.userId = :userId OR e.user2.userId = :userId " +
            "GROUP BY e.h3Index")
    List<H3CountRow> countByH3IndexForUser(@Param("userId") Long userId);
}
