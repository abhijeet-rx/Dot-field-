package com.dotfield.security;

import com.dotfield.entity.Profile;
import com.dotfield.entity.User;
import com.dotfield.exception.ResourceNotFoundException;
import com.dotfield.exception.UnauthorizedException;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized helper for retrieving the currently authenticated User and their candidate Profile.
 * Prevents repeating SecurityContext extraction across controllers and services.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public Profile getCurrentUserProfile() {
        Long userId = getCurrentUserId();
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found for current user"));
    }
}
