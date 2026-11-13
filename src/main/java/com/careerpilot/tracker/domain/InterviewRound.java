package com.careerpilot.tracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_rounds")
public class InterviewRound {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "round_type", nullable = false)
    private String roundType;

    @Column(nullable = false)
    private String outcome;

    @Column(name = "preparation_notes", columnDefinition = "TEXT")
    private String preparationNotes;

    @Column(name = "questions_asked", columnDefinition = "TEXT")
    private String questionsAsked;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Version
    private Long version;

    protected InterviewRound() {
    }

    InterviewRound(JobApplication application, int roundNumber, String roundType, Instant scheduledAt, String preparationNotes) {
        this.id = UUID.randomUUID();
        this.application = application;
        this.roundNumber = roundNumber;
        this.roundType = roundType;
        this.outcome = "PENDING";
        this.scheduledAt = scheduledAt;
        this.preparationNotes = preparationNotes;
    }

    void recordOutcome(String outcome, String questionsAsked) {
        this.outcome = outcome;
        this.questionsAsked = questionsAsked;
    }

    public UUID getId() { return id; }
    public int getRoundNumber() { return roundNumber; }
    public String getRoundType() { return roundType; }
    public String getOutcome() { return outcome; }
    public String getPreparationNotes() { return preparationNotes; }
    public String getQuestionsAsked() { return questionsAsked; }
    public Instant getScheduledAt() { return scheduledAt; }
}
