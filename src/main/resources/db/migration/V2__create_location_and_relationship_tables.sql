-- ============================================================
-- V2: 위치 도메인 + 관계 도메인 + 채팅
-- ============================================================

-- 3-1. excluded_areas
CREATE TABLE excluded_areas (
    excluded_area_id    BIGSERIAL PRIMARY KEY,
    user_id             BIGINT              NOT NULL REFERENCES users(user_id),
    name                VARCHAR(50)         NOT NULL,
    latitude            DOUBLE PRECISION    NOT NULL,
    longitude           DOUBLE PRECISION    NOT NULL,
    radius              INT                 NOT NULL,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_excluded_areas_user_id ON excluded_areas(user_id);

-- 3-2. excluded_area_h3_indexes
CREATE TABLE excluded_area_h3_indexes (
    id                  BIGSERIAL PRIMARY KEY,
    excluded_area_id    BIGINT          NOT NULL REFERENCES excluded_areas(excluded_area_id) ON DELETE CASCADE,
    h3_index            VARCHAR(20)     NOT NULL
);

CREATE INDEX idx_excluded_h3_area_id ON excluded_area_h3_indexes(excluded_area_id);
CREATE INDEX idx_excluded_h3_index ON excluded_area_h3_indexes(h3_index);

-- 4-1. relationships
CREATE TABLE relationships (
    relationship_id     BIGSERIAL PRIMARY KEY,
    user_id_1           BIGINT          NOT NULL REFERENCES users(user_id),
    user_id_2           BIGINT          NOT NULL REFERENCES users(user_id),
    status              VARCHAR(20)     NOT NULL,
    encounter_count     INT             NOT NULL DEFAULT 0,
    first_encounter_at  TIMESTAMPTZ,
    last_encounter_at   TIMESTAMPTZ,
    status_changed_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ,
    chat_room_id        VARCHAR(100),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id_1, user_id_2),
    CHECK(user_id_1 < user_id_2)
);

CREATE INDEX idx_relationships_user1 ON relationships(user_id_1);
CREATE INDEX idx_relationships_user2 ON relationships(user_id_2);
CREATE INDEX idx_relationships_status ON relationships(status);
CREATE INDEX idx_relationships_expires ON relationships(expires_at) WHERE expires_at IS NOT NULL;

-- 4-2. encounters
CREATE TABLE encounters (
    encounter_id        BIGSERIAL PRIMARY KEY,
    relationship_id     BIGINT              NOT NULL REFERENCES relationships(relationship_id),
    user_id_1           BIGINT              NOT NULL REFERENCES users(user_id),
    user_id_2           BIGINT              NOT NULL REFERENCES users(user_id),
    h3_index            VARCHAR(20)         NOT NULL,
    latitude            DOUBLE PRECISION    NOT NULL,
    longitude           DOUBLE PRECISION    NOT NULL,
    distance            DOUBLE PRECISION    NOT NULL,
    encounter_number    INT                 NOT NULL,
    batch_window_at     TIMESTAMPTZ         NOT NULL,
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_encounters_relationship ON encounters(relationship_id);
CREATE INDEX idx_encounters_batch ON encounters(batch_window_at);

-- 4-3. inyeon_requests
CREATE TABLE inyeon_requests (
    request_id          BIGSERIAL PRIMARY KEY,
    relationship_id     BIGINT          NOT NULL REFERENCES relationships(relationship_id),
    requester_id        BIGINT          NOT NULL REFERENCES users(user_id),
    receiver_id         BIGINT          NOT NULL REFERENCES users(user_id),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    knots_used          INT             NOT NULL DEFAULT 0,
    responded_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inyeon_requests_relationship ON inyeon_requests(relationship_id);
CREATE INDEX idx_inyeon_requests_receiver ON inyeon_requests(receiver_id, status);

-- 4-4. restore_requests
CREATE TABLE restore_requests (
    restore_request_id  BIGSERIAL PRIMARY KEY,
    relationship_id     BIGINT          NOT NULL REFERENCES relationships(relationship_id),
    requester_id        BIGINT          NOT NULL REFERENCES users(user_id),
    receiver_id         BIGINT          NOT NULL REFERENCES users(user_id),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    knots_used          INT             NOT NULL DEFAULT 30,
    responded_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restore_requests_relationship ON restore_requests(relationship_id);
CREATE INDEX idx_restore_requests_receiver ON restore_requests(receiver_id, status);

-- 4-5. relationship_logs
CREATE TABLE relationship_logs (
    log_id              BIGSERIAL PRIMARY KEY,
    relationship_id     BIGINT          NOT NULL REFERENCES relationships(relationship_id),
    from_status         VARCHAR(20)     NOT NULL,
    to_status           VARCHAR(20)     NOT NULL,
    triggered_by        VARCHAR(50)     NOT NULL,
    triggered_user_id   BIGINT          REFERENCES users(user_id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_relationship_logs_relationship ON relationship_logs(relationship_id);
CREATE INDEX idx_relationship_logs_created ON relationship_logs(created_at);

-- 5-1. chat_rooms
CREATE TABLE chat_rooms (
    chat_room_id        BIGSERIAL PRIMARY KEY,
    firestore_room_id   VARCHAR(100)    NOT NULL UNIQUE,
    relationship_id     BIGINT          NOT NULL REFERENCES relationships(relationship_id),
    user_id_1           BIGINT          NOT NULL REFERENCES users(user_id),
    user_id_2           BIGINT          NOT NULL REFERENCES users(user_id),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMPTZ     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_rooms_relationship ON chat_rooms(relationship_id);
CREATE INDEX idx_chat_rooms_users ON chat_rooms(user_id_1, user_id_2);
