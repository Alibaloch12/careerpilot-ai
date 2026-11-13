package com.careerpilot.tracker.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByUserIdOrderByAppliedAtDesc(UUID userId);
    List<JobApplication> findByUserIdAndStatusOrderByAppliedAtDesc(UUID userId, ApplicationStatus status);
    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);
}
