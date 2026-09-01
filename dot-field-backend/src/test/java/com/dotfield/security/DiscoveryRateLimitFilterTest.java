package com.dotfield.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryRateLimitFilterTest {

    @Test
    @DisplayName("Requests within capacity limit pass through filter successfully")
    void requestsWithinCapacitySucceed() throws Exception {
        DiscoveryRateLimitFilter filter = new DiscoveryRateLimitFilter(new ObjectMapper(), 2, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs/discover");
        request.setRemoteAddr("192.168.1.100");

        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockFilterChain filterChain1 = new MockFilterChain();
        filter.doFilter(request, response1, filterChain1);
        assertThat(response1.getStatus()).isNotEqualTo(429);

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();
        filter.doFilter(request, response2, filterChain2);
        assertThat(response2.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("Requests exceeding capacity limit receive HTTP 429 Too Many Requests with Retry-After header")
    void requestsExceedingCapacityReceive429() throws Exception {
        DiscoveryRateLimitFilter filter = new DiscoveryRateLimitFilter(new ObjectMapper(), 1, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs/discover");
        request.setRemoteAddr("10.0.0.1");

        // First request consumes the single token
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        filter.doFilter(request, response1, new MockFilterChain());
        assertThat(response1.getStatus()).isNotEqualTo(429);

        // Second request exceeds limit
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        filter.doFilter(request, response2, new MockFilterChain());

        assertThat(response2.getStatus()).isEqualTo(429);
        assertThat(response2.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response2.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Non-discovery endpoints bypass rate limiter filter")
    void nonDiscoveryEndpointsBypassFilter() throws Exception {
        DiscoveryRateLimitFilter filter = new DiscoveryRateLimitFilter(new ObjectMapper(), 1, 60);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");
        request.setRemoteAddr("10.0.0.2");

        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockFilterChain filterChain1 = new MockFilterChain();
        filter.doFilter(request, response1, filterChain1);

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();
        filter.doFilter(request, response2, filterChain2);

        assertThat(response1.getStatus()).isEqualTo(200);
        assertThat(response2.getStatus()).isEqualTo(200);
    }
}
