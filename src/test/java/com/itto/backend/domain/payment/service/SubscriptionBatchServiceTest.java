package com.itto.backend.domain.payment.service;

import com.itto.backend.domain.payment.entity.Subscription;
import com.itto.backend.domain.payment.repository.SubscriptionRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.enums.Store;
import com.itto.backend.global.enums.SubscriptionPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * D9: SubscriptionBatchService unit tests
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionBatchServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionBatchService subscriptionBatchService;

    private User buildPremiumUser(Long id) {
        return User.builder()
                .userId(id)
                .firebaseUid("firebase-" + id)
                .provider(Provider.KAKAO)
                .isPremium(true)
                .knots(0)
                .build();
    }

    private Subscription buildExpiredSubscription(User user) {
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
    @DisplayName("Single expired subscription: deactivate and set user non-premium")
    void processExpired_singleSubscription() {
        User user = buildPremiumUser(1L);
        Subscription sub = buildExpiredSubscription(user);

        when(subscriptionRepository.findByIsActiveTrueAndExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(sub));

        int count = subscriptionBatchService.processExpiredSubscriptions();

        assertThat(count).isEqualTo(1);
        assertThat(sub.getIsActive()).isFalse();
        assertThat(user.getIsPremium()).isFalse();
    }

    @Test
    @DisplayName("Multiple expired subscriptions: all processed")
    void processExpired_multipleSubscriptions() {
        User user1 = buildPremiumUser(1L);
        User user2 = buildPremiumUser(2L);
        Subscription sub1 = buildExpiredSubscription(user1);
        Subscription sub2 = buildExpiredSubscription(user2);

        when(subscriptionRepository.findByIsActiveTrueAndExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(sub1, sub2));

        int count = subscriptionBatchService.processExpiredSubscriptions();

        assertThat(count).isEqualTo(2);
        assertThat(sub1.getIsActive()).isFalse();
        assertThat(user1.getIsPremium()).isFalse();
        assertThat(sub2.getIsActive()).isFalse();
        assertThat(user2.getIsPremium()).isFalse();
    }

    @Test
    @DisplayName("No expired subscriptions: returns 0")
    void processExpired_noExpired() {
        when(subscriptionRepository.findByIsActiveTrueAndExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        int count = subscriptionBatchService.processExpiredSubscriptions();

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Partial failure: other subscriptions continue processing")
    void processExpired_partialFailure() {
        User badUser = mock(User.class);
        doThrow(new RuntimeException("DB error")).when(badUser).updatePremium(false);

        Subscription sub1 = buildExpiredSubscription(badUser);
        User user2 = buildPremiumUser(2L);
        Subscription sub2 = buildExpiredSubscription(user2);

        when(subscriptionRepository.findByIsActiveTrueAndExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(sub1, sub2));

        int count = subscriptionBatchService.processExpiredSubscriptions();

        // sub1 failed, sub2 succeeded
        assertThat(count).isEqualTo(1);
        assertThat(sub2.getIsActive()).isFalse();
        assertThat(user2.getIsPremium()).isFalse();
    }
}
