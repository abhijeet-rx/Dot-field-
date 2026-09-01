package com.dotfield.service;

import com.dotfield.dto.AuthResponse;
import com.dotfield.dto.LoginRequest;
import com.dotfield.dto.RegisterRequest;
import com.dotfield.dto.UserResponse;
import com.dotfield.entity.Profile;
import com.dotfield.entity.Role;
import com.dotfield.entity.User;
import com.dotfield.exception.BadRequestException;
import com.dotfield.exception.UnauthorizedException;
import com.dotfield.repository.ProfileRepository;
import com.dotfield.repository.UserRepository;
import com.dotfield.security.CurrentUserService;
import com.dotfield.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    @Value("${initial.admin.email:}")
    private String initialAdminEmail;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("An account with email '" + normalizedEmail + "' already exists");
        }

        Role role = Role.USER;
        if (initialAdminEmail != null && !initialAdminEmail.trim().isEmpty() 
                && initialAdminEmail.trim().equalsIgnoreCase(normalizedEmail)) {
            role = Role.ADMIN;
            log.info("Assigning ADMIN role to initial admin user: {}", normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-create associated candidate Profile for the user
        String profileName = request.getName() != null && !request.getName().trim().isEmpty()
                ? request.getName().trim()
                : normalizedEmail.split("@")[0];

        Profile profile = Profile.builder()
                .user(savedUser)
                .name(profileName)
                .email(normalizedEmail)
                .build();

        Profile savedProfile = profileRepository.save(profile);
        log.info("Registered new user ID: {} with candidate Profile ID: {} and Role: {}", savedUser.getId(), savedProfile.getId(), savedUser.getRole());

        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .profileId(savedProfile.getId())
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        Optional<Profile> profileOpt = profileRepository.findByUserId(user.getId());
        Long profileId = profileOpt.map(Profile::getId).orElse(null);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileId(profileId)
                .build();

        log.info("User ID: {} logged in successfully with Role: {}", user.getId(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse() {
        User user = currentUserService.getCurrentUser();
        Optional<Profile> profileOpt = profileRepository.findByUserId(user.getId());
        Long profileId = profileOpt.map(Profile::getId).orElse(null);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileId(profileId)
                .build();
    }
}
