package com.dotfield.config;

import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Idempotent Admin Bootstrap Initializer.
 * Ensures the configured INITIAL_ADMIN_EMAIL user holds the ADMIN role on startup
 * without altering user passwords, generating fake profiles, or resetting accounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapInitializer {

    private final UserRepository userRepository;

    @Value("${initial.admin.email:}")
    private String initialAdminEmail;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        if (initialAdminEmail == null || initialAdminEmail.isBlank()) {
            log.info("AdminBootstrapInitializer: No INITIAL_ADMIN_EMAIL configured.");
            return;
        }

        String targetEmail = initialAdminEmail.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(targetEmail);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("AdminBootstrapInitializer: Ensured ADMIN role for configured admin identity: {}", targetEmail);
            } else {
                log.info("AdminBootstrapInitializer: Configured admin user '{}' already holds ADMIN role.", targetEmail);
            }
        } else {
            log.info("AdminBootstrapInitializer: Configured admin email '{}' is not registered in database yet.", targetEmail);
        }
    }
}
