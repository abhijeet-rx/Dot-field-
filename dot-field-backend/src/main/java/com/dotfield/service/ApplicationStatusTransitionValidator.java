package com.dotfield.service;

import com.dotfield.entity.ApplicationStatus;
import com.dotfield.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ApplicationStatusTransitionValidator {

    private static final Set<ApplicationStatus> VALID_CREATION_STATUSES =
            EnumSet.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED);

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ApplicationStatus.SAVED, EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.APPLIED, EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.SCREENING, EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.INTERVIEW, EnumSet.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER, EnumSet.of(ApplicationStatus.WITHDRAWN)
    );

    /**
     * Validates that the requested initial status is allowed for application creation.
     * Only SAVED and APPLIED are valid creation statuses.
     */
    public void validateCreationStatus(ApplicationStatus status) {
        if (status != null && !VALID_CREATION_STATUSES.contains(status)) {
            throw new BadRequestException(
                    "Invalid initial application status: " + status + ". Allowed creation statuses are: " + VALID_CREATION_STATUSES);
        }
    }

    public void validateTransition(ApplicationStatus currentStatus, ApplicationStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == ApplicationStatus.REJECTED || currentStatus == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("Cannot transition application out of terminal state: " + currentStatus);
        }

        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }
}

