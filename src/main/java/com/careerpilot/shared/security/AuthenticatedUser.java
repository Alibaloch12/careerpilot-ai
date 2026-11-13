package com.careerpilot.shared.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        String role
) {
}
