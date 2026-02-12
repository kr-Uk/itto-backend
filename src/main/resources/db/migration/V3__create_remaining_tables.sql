-- ============================================================
-- V3: 알림 + 차단/신고 + 결제 + 시스템
-- ============================================================

-- 6-1. notifications
CREATE TABLE notifications (
    notification_id     BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    type                VARCHAR(30)     NOT NULL,
    title               VARCHAR(100)    NOT NULL,
    message             VARCHAR(300)    NOT NULL,
    deep_link           VARCHAR(200),
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at);

-- 7-1. blocks
CREATE TABLE blocks (
    block_id            BIGSERIAL PRIMARY KEY,
    blocker_id          BIGINT          NOT NULL REFERENCES users(user_id),
    blocked_id          BIGINT          NOT NULL REFERENCES users(user_id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id)
);

CREATE INDEX idx_blocks_blocker ON blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON blocks(blocked_id);

-- 7-2. reports
CREATE TABLE reports (
    report_id           BIGSERIAL PRIMARY KEY,
    reporter_id         BIGINT          NOT NULL REFERENCES users(user_id),
    target_user_id      BIGINT          NOT NULL REFERENCES users(user_id),
    category            VARCHAR(30)     NOT NULL,
    target_type         VARCHAR(20)     NOT NULL,
    chat_room_id        VARCHAR(100),
    description         VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'RECEIVED',
    resolved_by         BIGINT          REFERENCES users(user_id),
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_target ON reports(target_user_id);
CREATE INDEX idx_reports_status ON reports(status);

-- 8-1. knot_transactions
CREATE TABLE knot_transactions (
    transaction_id      BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    type                VARCHAR(30)     NOT NULL,
    amount              INT             NOT NULL,
    balance_after       INT             NOT NULL,
    reference_id        VARCHAR(100),
    description         VARCHAR(200),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_knot_transactions_user ON knot_transactions(user_id);
CREATE INDEX idx_knot_transactions_created ON knot_transactions(created_at);

-- 8-2. purchases
CREATE TABLE purchases (
    purchase_id         BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    package_name        VARCHAR(50)     NOT NULL,
    price               INT             NOT NULL,
    knots_granted       INT             NOT NULL,
    is_first_purchase   BOOLEAN         NOT NULL DEFAULT FALSE,
    store               VARCHAR(20)     NOT NULL,
    receipt_token       VARCHAR(1000)   NOT NULL,
    receipt_validated    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchases_user ON purchases(user_id);

-- 8-3. subscriptions
CREATE TABLE subscriptions (
    subscription_id         BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(user_id),
    store                   VARCHAR(20)     NOT NULL,
    store_subscription_id   VARCHAR(200),
    plan                    VARCHAR(30)     NOT NULL DEFAULT 'MONTHLY',
    price                   INT             NOT NULL,
    is_promotion            BOOLEAN         NOT NULL DEFAULT FALSE,
    started_at              TIMESTAMPTZ     NOT NULL,
    expires_at              TIMESTAMPTZ     NOT NULL,
    cancelled_at            TIMESTAMPTZ,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_active ON subscriptions(is_active, expires_at);

-- 8-4. monthly_usage
CREATE TABLE monthly_usage (
    monthly_usage_id    BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    year_month          VARCHAR(7)      NOT NULL,
    free_converts_used  INT             NOT NULL DEFAULT 0,
    free_requests_used  INT             NOT NULL DEFAULT 0,
    free_restores_used  INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, year_month)
);

CREATE INDEX idx_monthly_usage_user ON monthly_usage(user_id, year_month);

-- 8-5. first_purchase_flags
CREATE TABLE first_purchase_flags (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(user_id),
    package_name        VARCHAR(50)     NOT NULL,
    used_at             TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, package_name)
);

-- 9-1. app_versions
CREATE TABLE app_versions (
    version_id          BIGSERIAL PRIMARY KEY,
    platform            VARCHAR(20)     NOT NULL,
    min_version         VARCHAR(20)     NOT NULL,
    latest_version      VARCHAR(20)     NOT NULL,
    force_update        BOOLEAN         NOT NULL DEFAULT FALSE,
    update_url          VARCHAR(500),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 9-2. profanity_words
CREATE TABLE profanity_words (
    word_id             BIGSERIAL PRIMARY KEY,
    word                VARCHAR(50)     NOT NULL UNIQUE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
