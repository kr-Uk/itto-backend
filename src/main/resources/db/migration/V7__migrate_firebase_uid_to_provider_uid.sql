ALTER TABLE users RENAME COLUMN firebase_uid TO provider_uid;

DROP INDEX IF EXISTS idx_users_firebase_uid;

CREATE UNIQUE INDEX idx_users_provider_provider_uid ON users(provider, provider_uid) WHERE deleted_at IS NULL;

CREATE INDEX idx_users_provider_uid ON users(provider_uid);
