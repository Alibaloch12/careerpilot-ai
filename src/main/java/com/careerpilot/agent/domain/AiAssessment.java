package com.careerpilot.agent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_assessments")
public class AiAssessment {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "matching_skills", columnDefinition = "TEXT")
    private String matchingSkills;

    @Column(name = "tailoring_suggestions", nullable = false, columnDefinition = "TEXT")
    private String tailoringSuggestions;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected AiAssessment() {
    }

    public AiAssessment(
            UUID userId,
            UUID applicationId,
            UUID resumeId,
            int matchScore,
            String matchingSkills,
            String missingSkills,
            String tailoringSuggestions,
            String modelName
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.applicationId = applicationId;
        this.resumeId = resumeId;
        this.matchScore = matchScore;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.tailoringSuggestions = tailoringSuggestions;
        this.modelName = modelName;
        this.evaluatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getApplicationId() { return applicationId; }
    public UUID getResumeId() { return resumeId; }
    public int getMatchScore() { return matchScore; }
    public String getMatchingSkills() { return matchingSkills; }
    public String getMissingSkills() { return missingSkills; }
    public String getTailoringSuggestions() { return tailoringSuggestions; }
    public String getModelName() { return modelName; }
}
