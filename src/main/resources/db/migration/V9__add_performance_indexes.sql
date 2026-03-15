-- ============================================================
-- V9: 성능 최적화 인덱스 추가
-- ============================================================

-- 1. relationships: 배치 만료 처리 최적화 (status + expires_at 복합 인덱스)
-- findByStatusAndExpiresAtBefore() 쿼리에서 status별 만료 조회 시 사용
-- 기존 idx_relationships_status (status만)와 idx_relationships_expires (expires_at만)는
-- 복합 조건에서 비효율적 → 복합 인덱스로 커버링 인덱스 효과
CREATE INDEX idx_relationships_status_expires
    ON relationships(status, expires_at)
    WHERE expires_at IS NOT NULL;

-- 2. notifications: 사용자별 알림 목록 조회 최적화
-- findByUserUserIdOrderByCreatedAtDesc() 정렬 인덱스
CREATE INDEX idx_notifications_user_created
    ON notifications(user_id, created_at DESC);

-- 3. blocks: 양방향 차단 체크 최적화
-- existsBlockBetween() 쿼리 (OR 조건)에서 blocked_id → blocker_id 방향도 커버
CREATE INDEX idx_blocks_blocked_blocker
    ON blocks(blocked_id, blocker_id);

-- 4. user_photos: 대표사진 조회 최적화
-- findByUserUserIdAndIsRepresentativeTrue() 쿼리 최적화
CREATE INDEX idx_user_photos_user_representative
    ON user_photos(user_id, is_representative)
    WHERE is_representative = TRUE;
