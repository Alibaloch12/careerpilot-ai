package com.careerpilot.shared.event;

import java.util.UUID;

public record ResumeUploadedEvent(
        UUID resumeId,
        UUID userId,
        String versionName,
        String rawContent
) {
}
