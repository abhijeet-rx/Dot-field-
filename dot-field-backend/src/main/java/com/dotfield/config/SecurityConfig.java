package com.dotfield.config;

import com.dotfield.exception.ApiError;
import com.dotfield.security.DiscoveryRateLimitFilter;
import com.dotfield.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DiscoveryRateLimitFilter discoveryRateLimitFilter;
    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/auth/register", "/auth/login", "/health").permitAll()

                        // Admin-only endpoints (Job Ingestion & Mutation)
                        .requestMatchers(HttpMethod.POST, "/jobs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/jobs/extract").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/jobs/discover").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/jobs/ingestion/run").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/jobs/ingestion/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/jobs/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/jobs/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/jobs/*").hasRole("ADMIN")

                        // Authenticated endpoints — read-only job access for USER or ADMIN
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/**").authenticated()

                        // Authenticated endpoints — profile, auth, and application tracking
                        .requestMatchers("/auth/me", "/profile/**", "/applications/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Filter ordering: JwtAuthenticationFilter MUST run BEFORE DiscoveryRateLimitFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(discoveryRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.contains("*")) {
            throw new IllegalArgumentException("Wildcard '*' CORS origin is strictly prohibited when allowCredentials is enabled");
        }

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized request to '{}': {}", request.getRequestURI(), authException.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiError error = ApiError.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("Unauthorized access. Please provide a valid Bearer token.")
                    .timestamp(LocalDateTime.now())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied for request to '{}': {}", request.getRequestURI(), accessDeniedException.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiError error = ApiError.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Access denied: You do not have permission to access this resource")
                    .timestamp(LocalDateTime.now())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }
}
