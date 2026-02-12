-- V4: 로그인 시 신규 유저는 프로필 미입력 상태로 생성되므로
-- nickname, gender, birth_date를 nullable로 변경
ALTER TABLE users ALTER COLUMN nickname DROP NOT NULL;
ALTER TABLE users ALTER COLUMN gender DROP NOT NULL;
ALTER TABLE users ALTER COLUMN birth_date DROP NOT NULL;
