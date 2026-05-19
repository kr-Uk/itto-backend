-- ============================================================
-- V11: 채팅 자체 호스팅 (Firestore → Spring WebSocket + PostgreSQL)
--
-- 변경 사항:
--   1) chat_rooms: firestore_room_id 컬럼 제거, 마지막 메시지 메타데이터 컬럼 추가
--   2) chat_messages: 신규 테이블 (이전엔 Firestore에 있던 메시지 데이터를 PostgreSQL로 이전)
--
-- relationships.chat_room_id (VARCHAR 100) 컬럼은 그대로 유지하며
-- 새 chat_rooms.chat_room_id (BIGINT)의 문자열 표현을 저장합니다.
-- ============================================================

-- 1. chat_rooms: Firestore 잔재 제거 + 메타데이터 추가
ALTER TABLE chat_rooms DROP COLUMN firestore_room_id;
ALTER TABLE chat_rooms ADD COLUMN last_message    TEXT;
ALTER TABLE chat_rooms ADD COLUMN last_message_at TIMESTAMPTZ;

-- 2. chat_messages: 메시지 본체
CREATE TABLE chat_messages (
    message_id      BIGSERIAL PRIMARY KEY,
    chat_room_id    BIGINT          NOT NULL REFERENCES chat_rooms(chat_room_id) ON DELETE CASCADE,
    sender_id       BIGINT          NOT NULL REFERENCES users(user_id),
    content         TEXT,
    image_url       VARCHAR(500),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 최근 메시지부터 페이지네이션
CREATE INDEX idx_chat_messages_room_created ON chat_messages(chat_room_id, created_at DESC);
-- 미읽음 카운트 빠른 조회
CREATE INDEX idx_chat_messages_room_unread  ON chat_messages(chat_room_id) WHERE is_read = FALSE;
