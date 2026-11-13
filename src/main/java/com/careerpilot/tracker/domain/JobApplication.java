package com.careerpilot.tracker.domain;

import com.careerpilot.shared.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "role_title", nullable = false)
    private String roleTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false)
    private WorkMode workMode;

    @Embedded
    private SalaryRange salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "last_status_change_at", nullable = false)
    private Instant lastStatusChangeAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewRound> interviewRounds = new ArrayList<>();

    protected JobApplication() {
    }

    public JobApplication(
            UUID userId,
            UUID resumeId,
            String companyName,
            String roleTitle,
            WorkMode workMode,
            SalaryRange salaryRange,
            String jobDescription
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.resumeId = resumeId;
        this.companyName = companyName;
        this.roleTitle = roleTitle;
        this.workMode = workMode;
        this.salaryRange = salaryRange;
        this.status = ApplicationStatus.APPLIED;
        this.jobDescription = jobDescription;
        this.appliedAt = Instant.now();
        this.lastStatusChangeAt = Instant.now();
    }

    public void transitionStatus(ApplicationStatus newStatus) {
        if (this.status == ApplicationStatus.WITHDRAWN && newStatus != ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot reopen a withdrawn application");
        }
        this.status = newStatus;
        this.lastStatusChangeAt = Instant.now();
    }

    public InterviewRound scheduleInterviewRound(String roundType, Instant scheduledAt, String preparationNotes) {
        if (this.status == ApplicationStatus.REJECTED || this.status == ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Cannot schedule an interview for a closed application");
        }
        this.status = ApplicationStatus.INTERVIEWING;
        this.lastStatusChangeAt = Instant.now();
        int nextRound = this.interviewRounds.size() + 1;
        InterviewRound round = new InterviewRound(this, nextRound, roundType, scheduledAt, preparationNotes);
        this.interviewRounds.add(round);
        return round;
    }

    public InterviewRound recordInterviewOutcome(UUID roundId, String outcome, String questionsAsked) {
        InterviewRound round = this.interviewRounds.stream()
                .filter(r -> r.getId().equals(roundId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Interview round not found: " + roundId));
        round.recordOutcome(outcome, questionsAsked);
        this.lastStatusChangeAt = Instant.now();
        return round;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getResumeId() { return resumeId; }
    public String getCompanyName() { return companyName; }
    public String getRoleTitle() { return roleTitle; }
    public WorkMode getWorkMode() { return workMode; }
    public SalaryRange getSalaryRange() { return salaryRange; }
    public ApplicationStatus getStatus() { return status; }
    public String getJobDescription() { return jobDescription; }
    public Instant getAppliedAt() { return appliedAt; }
    public Instant getLastStatusChangeAt() { return lastStatusChangeAt; }
    public List<InterviewRound> getInterviewRounds() { return Collections.unmodifiableList(interviewRounds); }
}
