-- 본인인증 결과 저장 테이블
CREATE TABLE identity_verifications (
    verification_id   BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(user_id),
    request_no        VARCHAR(64)  NOT NULL UNIQUE,
    enc_data          TEXT,
    token_version_id  VARCHAR(64),
    integrity_value   TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    verified_name     VARCHAR(50),
    verified_phone    VARCHAR(20),
    verified_gender   VARCHAR(10),
    verified_birth_date DATE,
    ci                VARCHAR(256),
    di                VARCHAR(256),
    carrier           VARCHAR(20),
    error_code        VARCHAR(20),
    error_message     VARCHAR(200),
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_identity_verifications_user_id ON identity_verifications(user_id);

-- users 테이블에 본인인증 관련 컬럼 추가
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
ALTER TABLE users ADD COLUMN ci VARCHAR(256);
ALTER TABLE users ADD COLUMN di VARCHAR(256);
ALTER TABLE users ADD COLUMN identity_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 중복 가입 방지를 위한 unique partial index (ci, di가 NULL이 아닌 경우에만)
CREATE UNIQUE INDEX idx_users_ci_unique ON users(ci) WHERE ci IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_di_unique ON users(di) WHERE di IS NOT NULL AND deleted_at IS NULL;
