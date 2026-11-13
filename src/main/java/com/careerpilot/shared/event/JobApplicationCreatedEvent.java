package com.careerpilot.shared.event;

import java.util.UUID;

public record JobApplicationCreatedEvent(
        UUID applicationId,
        UUID userId,
        UUID resumeId,
        String companyName,
        String roleTitle,
        String jobDescription
) {
}
