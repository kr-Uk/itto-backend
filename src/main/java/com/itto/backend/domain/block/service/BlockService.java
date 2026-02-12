package com.itto.backend.domain.block.service;

import com.itto.backend.domain.block.dto.BlockListResponse;
import com.itto.backend.domain.block.dto.BlockRequest;
import com.itto.backend.domain.block.dto.ReportRequest;
import com.itto.backend.domain.block.dto.ReportResponse;
import com.itto.backend.domain.block.entity.Block;
import com.itto.backend.domain.block.entity.Report;
import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.block.repository.ReportRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    /**
     * 차단 목록 조회
     */
    public BlockListResponse getBlocks(Long userId) {
        List<Block> blocks = blockRepository.findByBlockerUserId(userId);

        List<BlockListResponse.BlockInfo> blockInfos = blocks.stream()
                .map(block -> BlockListResponse.BlockInfo.builder()
                        .id(block.getId())
                        .blockedUser(BlockListResponse.BlockedUserInfo.builder()
                                .userId(block.getBlocked().getUserId())
                                .nickname(block.getBlocked().getNickname())
                                .build())
                        .createdAt(block.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return BlockListResponse.builder()
                .blocks(blockInfos)
                .build();
    }

    /**
     * 사용자 차단
     */
    @Transactional
    public void blockUser(Long userId, BlockRequest request) {
        // 자기 자신 차단 불가
        if (userId.equals(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        // 중복 차단 체크
        if (blockRepository.existsByBlockerUserIdAndBlockedUserId(userId, request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.ALREADY_BLOCKED);
        }

        // 사용자 존재 여부 확인
        User blocker = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User blocked = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 차단 생성
        Block block = Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();

        blockRepository.save(block);
    }

    /**
     * 차단 해제
     */
    @Transactional
    public void unblock(Long userId, Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOCK_NOT_FOUND));

        // 본인의 차단만 해제 가능
        if (!block.getBlocker().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        blockRepository.delete(block);
    }

    /**
     * 신고 등록
     */
    @Transactional
    public ReportResponse report(Long userId, ReportRequest request) {
        // 사용자 존재 여부 확인
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 신고 생성
        Report report = Report.builder()
                .reporter(reporter)
                .targetUser(targetUser)
                .category(request.getCategory())
                .targetType(request.getTargetType())
                .chatRoomId(request.getChatRoomId())
                .description(request.getDescription())
                .build();

        Report savedReport = reportRepository.save(report);

        return ReportResponse.builder()
                .reportId(savedReport.getId())
                .message("신고가 접수되었습니다")
                .build();
    }
}
