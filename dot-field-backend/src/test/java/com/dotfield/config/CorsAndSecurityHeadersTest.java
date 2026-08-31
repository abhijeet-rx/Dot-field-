package com.dotfield.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsAndSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CORS preflight request from configured allowed origin (http://localhost:5173) succeeds with origin header")
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
    @DisplayName("Spring Security headers (X-Content-Type-Options, X-Frame-Options) are enforced on responses")
    void securityHeadersPresent() throws Exception {
        mockMvc.perform(options("/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
