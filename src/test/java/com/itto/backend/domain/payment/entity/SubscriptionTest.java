package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.enums.Store;
import com.itto.backend.domain.common.enums.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D9: Subscription entity expire() method tests
 */
class SubscriptionTest {

    private User buildUser() {
        return User.builder()
                .userId(1L)
                .providerUid("firebase-uid")
                .provider(Provider.KAKAO)
                .isPremium(true)
                .knots(0)
                .build();
    }

    private Subscription buildActiveSubscription(User user) {
        return Subscription.builder()
                .user(user)
                .store(Store.GOOGLE)
                .plan(SubscriptionPlan.MONTHLY)
                .price(6900)
                .startedAt(LocalDateTime.now().minusMonths(1))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("expire() sets isActive to false")
    void expire_shouldDeactivateSubscription() {
        Subscription subscription = buildActiveSubscription(buildUser());

        subscription.expire();

        assertThat(subscription.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("expire() is idempotent on already inactive subscription")
    void expire_onInactiveSubscription_shouldBeIdempotent() {
        Subscription subscription = buildActiveSubscription(buildUser());
        subscription.expire();

        // Second call should not throw
        subscription.expire();

        assertThat(subscription.getIsActive()).isFalse();
    }
}
