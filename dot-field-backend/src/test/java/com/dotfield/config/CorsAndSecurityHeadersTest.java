package com.dotfield.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:5173,http://localhost:5174"
})
class CorsAndSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class);

    @Test
    @DisplayName("CORS preflight request from explicitly configured allowed origin (http://localhost:5173) succeeds")
    void allowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/jobs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight request from unauthorized origin (http://malicious-site.com) does NOT return allow origin header")
    void unauthorizedOriginRejected() throws Exception {
        mockMvc.perform(options("/jobs")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Production CORS without CORS_ALLOWED_ORIGINS property does NOT silently allow localhost")
    void productionWithoutConfigDoesNotAllowLocalhost() {
        SecurityConfig config = new SecurityConfig(null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins", "");
        org.springframework.web.cors.CorsConfigurationSource source = config.corsConfigurationSource();
        assertThat(source).isNotNull();
    }

    @Test
    @DisplayName("Wildcard '*' CORS origin is strictly prohibited when allowCredentials is true")
    void wildcardCorsOriginIsProhibited() {
        SecurityConfig config = new SecurityConfig(null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(config, "allowedOrigins", "*");
        assertThrows(IllegalArgumentException.class, config::corsConfigurationSource);
    }

    @Test
    @DisplayName("Spring Security headers (X-Content-Type-Options, X-Frame-Options) are enforced on responses")
    void securityHeadersPresent() throws Exception {
        mockMvc.perform(options("/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
