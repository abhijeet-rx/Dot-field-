package com.dotfield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the Spring application context loads successfully.
 * Uses the 'test' profile which connects to an in-memory H2 database.
 */
@SpringBootTest
class DotFieldApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the application context started without errors.
    }

}
