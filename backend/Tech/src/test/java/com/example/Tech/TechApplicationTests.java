package com.example.Tech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the full context against the test database; with ddl-auto=validate this also
 * verifies that all JPA entities match database/techshopping.sql.
 */
@SpringBootTest
@ActiveProfiles("test")
class TechApplicationTests {

	@Test
	void contextLoads() {
	}

}
