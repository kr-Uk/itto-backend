package com.itto.backend.domain.relationship.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.enums.RelationshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D3: destinyBadge field behavior on Relationship entity.
 */
class RelationshipDestinyBadgeTest {

    @Test
    @DisplayName("D3: destinyBadge 기본값은 false")
    void defaultDestinyBadgeIsFalse() {
        User user1 = User.builder().userId(1L).providerUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).providerUid("fb-2").provider(Provider.KAKAO).build();

        Relationship rel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.UYEON)
                .statusChangedAt(LocalDateTime.now())
                .build();

        assertThat(rel.isDestinyBadge()).isFalse();
    }

    @Test
    @DisplayName("D3: markDestinyBadge()로 destinyBadge를 true로 설정")
    void markDestinyBadgeSetsTrue() {
        User user1 = User.builder().userId(1L).providerUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).providerUid("fb-2").provider(Provider.KAKAO).build();

        Relationship rel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.PILYEON)
                .encounterCount(2)
                .statusChangedAt(LocalDateTime.now())
                .build();

        rel.markDestinyBadge();

        assertThat(rel.isDestinyBadge()).isTrue();
    }

    @Test
    @DisplayName("D3: Builder로 destinyBadge(true) 설정 가능")
    void builderWithDestinyBadgeTrue() {
        User user1 = User.builder().userId(1L).providerUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).providerUid("fb-2").provider(Provider.KAKAO).build();

        Relationship rel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.INYEON)
                .encounterCount(3)
                .destinyBadge(true)
                .statusChangedAt(LocalDateTime.now())
                .build();

        assertThat(rel.isDestinyBadge()).isTrue();
    }
}
