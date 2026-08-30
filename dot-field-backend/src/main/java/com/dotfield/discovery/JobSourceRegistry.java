package com.dotfield.discovery;

import com.dotfield.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JobSourceRegistry {

    private final List<JobSource> sources;

    public Optional<JobSource> getSource(String sourceName) {
        if (sourceName == null || sourceName.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = sourceName.trim().toUpperCase();
        return sources.stream()
                .filter(s -> s.supports(normalized) || s.supports(sourceName.trim()))
                .findFirst();
    }

    public JobSource getRequiredSource(String sourceName) {
        return getSource(sourceName)
                .orElseThrow(() -> new BadRequestException("Unsupported job source: " + sourceName));
    }

}
