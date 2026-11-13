package com.careerpilot.agent.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiAssessmentRepository extends JpaRepository<AiAssessment, UUID> {
    List<AiAssessment> findByApplicationId(UUID applicationId);
}
