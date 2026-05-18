-- NICE 본인인증 제거 마이그레이션
-- phone_number 컬럼은 향후 SMS 인증을 위해 유지하고 phone_verified로 의미 전환

-- 1. NICE 결과 저장 테이블 제거
DROP TABLE IF EXISTS identity_verifications;

-- 2. NICE 전용 unique index 제거
DROP INDEX IF EXISTS idx_users_ci_unique;
DROP INDEX IF EXISTS idx_users_di_unique;

-- 3. NICE 전용 컬럼 제거
ALTER TABLE users DROP COLUMN IF EXISTS ci;
ALTER TABLE users DROP COLUMN IF EXISTS di;

-- 4. identity_verified → phone_verified 로 의미 전환 (값은 모두 false로 리셋)
ALTER TABLE users RENAME COLUMN identity_verified TO phone_verified;
UPDATE users SET phone_verified = FALSE;
