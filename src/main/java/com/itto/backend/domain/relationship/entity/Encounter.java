package com.itto.backend.domain.relationship.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "encounters",
    indexes = {
        @Index(name = "idx_encounters_relationship", columnList = "relationship_id"),
        @Index(name = "idx_encounters_batch", columnList = "batch_window_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Encounter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "encounter_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private Relationship relationship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User user2;

    @Column(name = "h3_index", nullable = false, length = 20)
    private String h3Index;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "distance", nullable = false)
    private Double distance;

    @Column(name = "encounter_number", nullable = false)
    private Integer encounterNumber;

    @Column(name = "batch_window_at", nullable = false)
    private LocalDateTime batchWindowAt;
}
