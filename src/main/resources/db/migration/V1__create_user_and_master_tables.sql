-- ============================================================
-- V1: 사용자 도메인 + 마스터 데이터
-- ============================================================

-- 1-1. users
CREATE TABLE users (
    user_id             BIGSERIAL PRIMARY KEY,
    firebase_uid        VARCHAR(128)    NOT NULL UNIQUE,
    provider            VARCHAR(20)     NOT NULL,
    nickname            VARCHAR(16)     NOT NULL,
    gender              VARCHAR(10)     NOT NULL,
    birth_date          DATE            NOT NULL,
    introduction        VARCHAR(30),
    knots               INT             NOT NULL DEFAULT 0,
    is_premium          BOOLEAN         NOT NULL DEFAULT FALSE,
    verification_status VARCHAR(20)     NOT NULL DEFAULT 'NONE',
    location_enabled    BOOLEAN         NOT NULL DEFAULT FALSE,
    role                VARCHAR(20)     NOT NULL DEFAULT 'USER',
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- 1-2. user_terms
CREATE TABLE user_terms (
    user_term_id        BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    service_terms       BOOLEAN         NOT NULL,
    privacy_policy      BOOLEAN         NOT NULL,
    location_terms      BOOLEAN         NOT NULL,
    marketing_consent   BOOLEAN         NOT NULL DEFAULT FALSE,
    terms_version       VARCHAR(20)     NOT NULL,
    agreed_at           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_terms_user_id ON user_terms(user_id);

-- 1-3. user_photos
CREATE TABLE user_photos (
    photo_id            BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    photo_url           VARCHAR(500)    NOT NULL,
    display_order       INT             NOT NULL,
    is_representative   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_photos_user_id ON user_photos(user_id);

-- 1-4. verifications
CREATE TABLE verifications (
    verification_id     BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    selfie_url          VARCHAR(500)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    reject_reason       VARCHAR(200),
    reviewed_by         BIGINT          REFERENCES users(user_id),
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verifications_user_id ON verifications(user_id);
CREATE INDEX idx_verifications_status ON verifications(status);

-- 1-5. fcm_tokens
CREATE TABLE fcm_tokens (
    fcm_token_id        BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    token               VARCHAR(500)    NOT NULL,
    device_id           VARCHAR(200)    NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_fcm_tokens_user_id ON fcm_tokens(user_id);

-- 1-6. user_settings
CREATE TABLE user_settings (
    user_setting_id             BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT      NOT NULL UNIQUE REFERENCES users(user_id),
    gender_filter               VARCHAR(10),
    age_range_min               INT         NOT NULL DEFAULT 20,
    age_range_max               INT         NOT NULL DEFAULT 60,
    chat_notification           BOOLEAN     NOT NULL DEFAULT TRUE,
    relationship_notification   BOOLEAN     NOT NULL DEFAULT TRUE,
    stats_notification          BOOLEAN     NOT NULL DEFAULT TRUE,
    marketing_notification      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2-1. hobbies (마스터 데이터)
CREATE TABLE hobbies (
    hobby_id            BIGSERIAL PRIMARY KEY,
    name                VARCHAR(50)     NOT NULL UNIQUE,
    category            VARCHAR(50)     NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 2-2. user_hobbies
CREATE TABLE user_hobbies (
    user_hobby_id       BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    hobby_id            BIGINT          NOT NULL REFERENCES hobbies(hobby_id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, hobby_id)
);

CREATE INDEX idx_user_hobbies_user_id ON user_hobbies(user_id);
