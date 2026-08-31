package com.dotfield.discovery;

import com.dotfield.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Registry for discovering and accessing registered {@link JobSource} implementations.
 * Uses Spring dependency injection to automatically discover all source beans.
 */
@Component
@RequiredArgsConstructor
public class JobSourceRegistry {

    private final List<JobSource> sources;

    /**
     * Resolves a registered job source adapter by source name.
     */
    public Optional<JobSource> getSource(String sourceName) {
        if (sourceName == null || sourceName.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = sourceName.trim().toUpperCase();
        return sources.stream()
                .filter(s -> s.supports(normalized) || s.supports(sourceName.trim()))
                .findFirst();
    }

    /**
     * Resolves a registered job source adapter or throws {@link BadRequestException}.
     */
    public JobSource getRequiredSource(String sourceName) {
        return getSource(sourceName)
                .orElseThrow(() -> new BadRequestException("Unsupported job source: " + sourceName));
    }

    /**
     * Returns an unmodifiable list of all registered job sources.
     */
    public List<JobSource> getAllSources() {
        return List.copyOf(sources);
    }

    /**
     * Returns names of all currently registered job sources.
     */
    public List<String> getAvailableSourceNames() {
        return sources.stream()
                .map(JobSource::getSourceName)
                .distinct()
                .toList();
    }

    /**
     * Checks whether a job source with the given name is registered.
     */
    public boolean isRegistered(String sourceName) {
        return getSource(sourceName).isPresent();
    }
}
