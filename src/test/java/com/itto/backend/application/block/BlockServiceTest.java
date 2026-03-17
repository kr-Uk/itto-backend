package com.itto.backend.application.block;

import com.itto.backend.domain.block.entity.Block;
import com.itto.backend.domain.block.entity.Report;
import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.block.repository.ReportRepository;
import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.enums.ReportCategory;
import com.itto.backend.domain.common.enums.ReportTargetType;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.presentation.dto.block.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockService")
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockService blockService;

    private User blocker;
    private User blocked;

    @BeforeEach
    void setUp() {
        blocker = User.builder()
                .providerUid("blocker-uid")
                .provider(Provider.GOOGLE)
                .nickname("blocker")
                .build();
        setField(blocker, "userId", 1L);

        blocked = User.builder()
                .providerUid("blocked-uid")
                .provider(Provider.GOOGLE)
                .nickname("blocked")
                .build();
        setField(blocked, "userId", 2L);
    }

    @Nested
    @DisplayName("getBlocks")
    class GetBlocksTest {

        @Test
        @DisplayName("차단 목록 조회 성공")
        void getBlocksSuccess() {
            // given
            Long userId = 1L;
            Block block = Block.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build();

            when(blockRepository.findByBlockerUserId(userId)).thenReturn(List.of(block));

            // when
            BlockListResponse response = blockService.getBlocks(userId);

            // then
            assertThat(response.getBlocks()).hasSize(1);
            assertThat(response.getBlocks().get(0).getBlockedUser().getNickname()).isEqualTo("blocked");
        }

        @Test
        @DisplayName("차단 목록이 비어 있는 경우")
        void getBlocksEmpty() {
            // given
            Long userId = 1L;
            when(blockRepository.findByBlockerUserId(userId)).thenReturn(List.of());

            // when
            BlockListResponse response = blockService.getBlocks(userId);

            // then
            assertThat(response.getBlocks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("blockUser")
    class BlockUserTest {

        @Test
        @DisplayName("차단 성공")
        void blockUserSuccess() {
            // given
            Long userId = 1L;
            BlockRequest request = BlockRequest.builder().targetUserId(2L).build();

            when(blockRepository.existsByBlockerUserIdAndBlockedUserId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(blocker));
            when(userRepository.findById(2L)).thenReturn(Optional.of(blocked));

            // when
            blockService.blockUser(userId, request);

            // then
            verify(blockRepository).save(any(Block.class));
        }

        @Test
        @DisplayName("자기 자신 차단 → BusinessException")
        void blockSelf() {
            // given
            Long userId = 1L;
            BlockRequest request = BlockRequest.builder().targetUserId(1L).build();

            // when & then
            assertThatThrownBy(() -> blockService.blockUser(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.CANNOT_BLOCK_SELF));
        }

        @Test
        @DisplayName("이미 차단한 유저 중복 차단 → BusinessException")
        void blockAlreadyBlocked() {
            // given
            Long userId = 1L;
            BlockRequest request = BlockRequest.builder().targetUserId(2L).build();

            when(blockRepository.existsByBlockerUserIdAndBlockedUserId(1L, 2L)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> blockService.blockUser(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_BLOCKED));
        }

        @Test
        @DisplayName("차단 대상 유저가 존재하지 않음 → BusinessException")
        void blockTargetNotFound() {
            // given
            Long userId = 1L;
            BlockRequest request = BlockRequest.builder().targetUserId(999L).build();

            when(blockRepository.existsByBlockerUserIdAndBlockedUserId(1L, 999L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(blocker));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> blockService.blockUser(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("unblock")
    class UnblockTest {

        @Test
        @DisplayName("차단 해제 성공")
        void unblockSuccess() {
            // given
            Long userId = 1L;
            Long blockId = 10L;
            Block block = Block.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build();

            when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));

            // when
            blockService.unblock(userId, blockId);

            // then
            verify(blockRepository).delete(block);
        }

        @Test
        @DisplayName("존재하지 않는 차단 해제 → BusinessException")
        void unblockNotFound() {
            // given
            Long userId = 1L;
            Long blockId = 999L;
            when(blockRepository.findById(blockId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> blockService.unblock(userId, blockId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BLOCK_NOT_FOUND));
        }

        @Test
        @DisplayName("본인의 차단이 아닌 경우 → BusinessException")
        void unblockForbidden() {
            // given
            Long otherUserId = 99L;
            Long blockId = 10L;
            Block block = Block.builder()
                    .blocker(blocker)  // blocker userId = 1L
                    .blocked(blocked)
                    .build();

            when(blockRepository.findById(blockId)).thenReturn(Optional.of(block));

            // when & then
            assertThatThrownBy(() -> blockService.unblock(otherUserId, blockId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("report")
    class ReportTest {

        @Test
        @DisplayName("신고 등록 성공")
        void reportSuccess() {
            // given
            Long userId = 1L;
            ReportRequest request = ReportRequest.builder()
                    .targetUserId(2L)
                    .category(ReportCategory.HARASSMENT)
                    .targetType(ReportTargetType.PROFILE)
                    .description("부적절한 행동")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(blocker));
            when(userRepository.findById(2L)).thenReturn(Optional.of(blocked));

            Report savedReport = Report.builder()
                    .reporter(blocker)
                    .targetUser(blocked)
                    .category(ReportCategory.HARASSMENT)
                    .targetType(ReportTargetType.PROFILE)
                    .description("부적절한 행동")
                    .build();
            setField(savedReport, "id", 100L);

            when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

            // when
            ReportResponse response = blockService.report(userId, request);

            // then
            assertThat(response.getReportId()).isEqualTo(100L);
            assertThat(response.getMessage()).isEqualTo("신고가 접수되었습니다");
        }

        @Test
        @DisplayName("신고자 유저가 존재하지 않음 → BusinessException")
        void reportReporterNotFound() {
            // given
            Long userId = 999L;
            ReportRequest request = ReportRequest.builder()
                    .targetUserId(2L)
                    .category(ReportCategory.SPAM)
                    .targetType(ReportTargetType.CHAT)
                    .build();

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> blockService.report(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("신고 대상 유저가 존재하지 않음 → BusinessException")
        void reportTargetNotFound() {
            // given
            Long userId = 1L;
            ReportRequest request = ReportRequest.builder()
                    .targetUserId(999L)
                    .category(ReportCategory.SPAM)
                    .targetType(ReportTargetType.CHAT)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(blocker));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> blockService.report(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // --- Helper ---

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            // Try superclass
            try {
                java.lang.reflect.Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set field: " + fieldName, ex);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
