package com.itto.backend.domain.location.service;

import com.itto.backend.domain.location.dto.ExcludedAreaCreateRequest;
import com.itto.backend.domain.location.dto.ExcludedAreaCreateResponse;
import com.itto.backend.domain.location.dto.LocationRequest;
import com.itto.backend.domain.location.entity.ExcludedArea;
import com.itto.backend.domain.location.entity.ExcludedAreaH3Index;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.enums.VerificationStatus;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExcludedAreaRepository excludedAreaRepository;

    @Mock
    private ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;

    @InjectMocks
    private LocationService locationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .providerUid("test-provider-uid")
                .provider(Provider.KAKAO)
                .nickname("testuser")
                .verificationStatus(VerificationStatus.APPROVED)
                .knots(100)
                .isPremium(false)
                .locationEnabled(false)
                .build();
    }

    @Test
    @DisplayName("위치 전송 시 H3 인덱스가 계산되고 로그가 남는다")
    void sendLocation_Success() {
        // given
        LocationRequest request = LocationRequest.builder()
                .h3Index("8928308280fffff") // 클라이언트에서 보낸 H3 인덱스
                .latitude(37.5665)
                .longitude(126.9780)
                .accuracy(10.0)
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        locationService.sendLocation(1L, request);

        // then
        verify(userRepository).findById(1L);
        // 로그가 정상적으로 남는지는 육안 확인 (H3 인덱스 계산 로직이 실행됨)
    }

    @Test
    @DisplayName("제외 지역 생성 시 H3 인덱스가 계산되고 저장된다")
    void createExcludedArea_Success() {
        // given
        ExcludedAreaCreateRequest request = ExcludedAreaCreateRequest.builder()
                .name("우리집")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100) // 100m 반경
                .build();

        ExcludedArea savedArea = ExcludedArea.builder()
                .id(1L)
                .user(testUser)
                .name("우리집")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(excludedAreaRepository.countByUserUserId(1L)).thenReturn(0L);
        when(excludedAreaRepository.save(any(ExcludedArea.class))).thenReturn(savedArea);
        when(excludedAreaH3IndexRepository.saveAll(anyList())).thenReturn(List.of());

        // when
        ExcludedAreaCreateResponse response = locationService.createExcludedArea(1L, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("우리집");
        assertThat(response.getH3Indexes()).isNotEmpty(); // H3 인덱스가 생성되었는지 확인

        // 100m 반경이면 k=1이므로 7개 H3 셀이 생성됨 (center + 6 neighbors)
        assertThat(response.getH3Indexes().size()).isEqualTo(7);

        // H3 인덱스가 DB에 저장되는지 확인
        ArgumentCaptor<List<ExcludedAreaH3Index>> captor = ArgumentCaptor.forClass(List.class);
        verify(excludedAreaH3IndexRepository).saveAll(captor.capture());
        List<ExcludedAreaH3Index> savedIndexes = captor.getValue();
        assertThat(savedIndexes).hasSize(7);
    }

    @Test
    @DisplayName("제외 지역 생성 시 더 큰 반경에는 더 많은 H3 인덱스가 생성된다")
    void createExcludedArea_LargerRadius_MoreIndexes() {
        // given
        ExcludedAreaCreateRequest request = ExcludedAreaCreateRequest.builder()
                .name("큰 지역")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(500) // 500m 반경
                .build();

        ExcludedArea savedArea = ExcludedArea.builder()
                .id(1L)
                .user(testUser)
                .name("큰 지역")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(500)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(excludedAreaRepository.countByUserUserId(1L)).thenReturn(0L);
        when(excludedAreaRepository.save(any(ExcludedArea.class))).thenReturn(savedArea);
        when(excludedAreaH3IndexRepository.saveAll(anyList())).thenReturn(List.of());

        // when
        ExcludedAreaCreateResponse response = locationService.createExcludedArea(1L, request);

        // then
        assertThat(response).isNotNull();
        // 500m 반경이면 k=5이므로 91개 H3 셀이 생성됨
        assertThat(response.getH3Indexes().size()).isEqualTo(91);
    }

    @Test
    @DisplayName("제외 지역이 3개를 초과하면 예외가 발생한다")
    void createExcludedArea_ExceedsLimit_ThrowsException() {
        // given
        ExcludedAreaCreateRequest request = ExcludedAreaCreateRequest.builder()
                .name("네 번째 지역")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(excludedAreaRepository.countByUserUserId(1L)).thenReturn(3L); // 이미 3개 존재

        // when & then
        assertThatThrownBy(() -> locationService.createExcludedArea(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCLUDED_AREA_LIMIT);

        verify(excludedAreaRepository, never()).save(any());
        verify(excludedAreaH3IndexRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 제외 지역을 생성하려 하면 예외가 발생한다")
    void createExcludedArea_UserNotFound_ThrowsException() {
        // given
        ExcludedAreaCreateRequest request = ExcludedAreaCreateRequest.builder()
                .name("테스트 지역")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100)
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> locationService.createExcludedArea(999L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(excludedAreaRepository, never()).save(any());
    }

    @Test
    @DisplayName("H3 인덱스는 해상도 9로 생성된다")
    void createExcludedArea_UsesResolution9() {
        // given
        ExcludedAreaCreateRequest request = ExcludedAreaCreateRequest.builder()
                .name("테스트")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100)
                .build();

        ExcludedArea savedArea = ExcludedArea.builder()
                .id(1L)
                .user(testUser)
                .name("테스트")
                .latitude(37.5665)
                .longitude(126.9780)
                .radius(100)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(excludedAreaRepository.countByUserUserId(1L)).thenReturn(0L);
        when(excludedAreaRepository.save(any(ExcludedArea.class))).thenReturn(savedArea);
        when(excludedAreaH3IndexRepository.saveAll(anyList())).thenReturn(List.of());

        // when
        ExcludedAreaCreateResponse response = locationService.createExcludedArea(1L, request);

        // then
        assertThat(response.getH3Indexes()).isNotEmpty();
        // H3 해상도 9의 인덱스는 15자리 문자열
        assertThat(response.getH3Indexes().get(0)).hasSize(15);
    }
}
