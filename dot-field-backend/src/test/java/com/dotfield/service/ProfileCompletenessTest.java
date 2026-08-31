package com.dotfield.service;

import com.dotfield.dto.ProfileCompletenessResponse;
import com.dotfield.entity.*;
import com.dotfield.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCompletenessTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileCompletenessService completenessService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("candidate@example.com").build();
    }

    @Test
    @DisplayName("calculateCompleteness — Partial profile score calculated accurately")
    void partialProfileCompleteness() {
        Profile profile = Profile.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .skills(List.of(Skill.builder().name("Java").build(), Skill.builder().name("Spring").build()))
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ProfileCompletenessResponse response = completenessService.calculateCompleteness(1L);

        assertNotNull(response);
        assertTrue(response.getScore() > 0);
        assertTrue(response.getScore() < 100);
        assertFalse(response.getMissingRecommendations().isEmpty());
    }

    @Test
    @DisplayName("calculateCompleteness — Full profile scores 100%")
    void fullProfileCompleteness() {
        Profile profile = Profile.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .location("San Francisco, CA")
                .skills(List.of(
                        Skill.builder().name("Java").build(),
                        Skill.builder().name("Spring").build(),
                        Skill.builder().name("SQL").build(),
                        Skill.builder().name("React").build(),
                        Skill.builder().name("Docker").build()
                ))
                .experience(List.of(
                        Experience.builder().company("Acme").role("Engineer").build(),
                        Experience.builder().company("Beta").role("Senior Engineer").build()
                ))
                .education(List.of(Education.builder().institution("MIT").degree("B.S.").build()))
                .projects(List.of(Project.builder().name("Portfolio").build()))
                .build();

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        ProfileCompletenessResponse response = completenessService.calculateCompleteness(1L);

        assertNotNull(response);
        assertEquals(100, response.getScore());
        assertTrue(response.getMissingRecommendations().isEmpty());
    }
}
