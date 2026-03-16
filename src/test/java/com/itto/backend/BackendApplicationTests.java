package com.itto.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("로컬에 PostgreSQL/Redis 없음 — CI 환경에서만 실행")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
