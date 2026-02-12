package com.itto.backend.domain.location.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "excluded_area_h3_indexes",
    indexes = {
        @Index(name = "idx_excluded_h3_area_id", columnList = "excluded_area_id"),
        @Index(name = "idx_excluded_h3_index", columnList = "h3_index")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExcludedAreaH3Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "excluded_area_id", nullable = false)
    private ExcludedArea excludedArea;

    @Column(name = "h3_index", nullable = false, length = 20)
    private String h3Index;
}
