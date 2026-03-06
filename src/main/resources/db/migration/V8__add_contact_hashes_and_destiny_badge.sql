-- ============================================================
-- V6: contact_hashes 테이블 생성 + relationships.destiny_badge 컬럼 추가
-- ============================================================

-- 1. contact_hashes 테이블
CREATE TABLE IF NOT EXISTS contact_hashes (
    contact_hash_id     BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    phone_hash          VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_phone_hash UNIQUE (user_id, phone_hash)
);

CREATE INDEX idx_contact_hashes_user ON contact_hashes(user_id);
CREATE INDEX idx_contact_hashes_phone_hash ON contact_hashes(phone_hash);

-- 2. relationships 테이블에 destiny_badge 컬럼 추가
ALTER TABLE relationships
    ADD COLUMN IF NOT EXISTS destiny_badge BOOLEAN NOT NULL DEFAULT FALSE;
