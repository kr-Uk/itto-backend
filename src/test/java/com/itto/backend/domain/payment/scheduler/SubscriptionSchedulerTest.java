package com.itto.backend.domain.payment.scheduler;

import com.itto.backend.domain.payment.service.SubscriptionBatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * D9: SubscriptionScheduler unit tests
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {

    @Mock
    private SubscriptionBatchService subscriptionBatchService;

    @InjectMocks
    private SubscriptionScheduler subscriptionScheduler;

    @Test
    @DisplayName("runSubscriptionExpiration delegates to batch service")
    void runSubscriptionExpiration_shouldCallBatchService() {
        when(subscriptionBatchService.processExpiredSubscriptions()).thenReturn(5);

        subscriptionScheduler.runSubscriptionExpiration();

        verify(subscriptionBatchService, times(1)).processExpiredSubscriptions();
    }

    @Test
    @DisplayName("Scheduler catches exception from batch service")
    void runSubscriptionExpiration_batchServiceThrows_shouldNotPropagate() {
        when(subscriptionBatchService.processExpiredSubscriptions())
                .thenThrow(new RuntimeException("DB down"));

        // Should not throw
        subscriptionScheduler.runSubscriptionExpiration();

        verify(subscriptionBatchService, times(1)).processExpiredSubscriptions();
    }
}
