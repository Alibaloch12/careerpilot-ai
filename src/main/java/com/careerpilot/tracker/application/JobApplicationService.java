package com.careerpilot.tracker.application;

import com.careerpilot.shared.event.JobApplicationCreatedEvent;
import com.careerpilot.shared.exception.ResourceNotFoundException;
import com.careerpilot.tracker.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public JobApplicationService(JobApplicationRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public record DashboardStats(
            long totalApplications,
            Map<ApplicationStatus, Long> byStatus,
            double interviewConversionRatePercent
    ) {}

    @Transactional
    public JobApplication createApplication(
            UUID userId,
            UUID resumeId,
            String companyName,
            String roleTitle,
            WorkMode workMode,
            Integer salaryMin,
            Integer salaryMax,
            String currency,
            String jobDescription
    ) {
        SalaryRange salaryRange = new SalaryRange(salaryMin, salaryMax, currency);
        JobApplication application = new JobApplication(
                userId, resumeId, companyName, roleTitle, workMode, salaryRange, jobDescription
        );
        JobApplication saved = repository.save(application);

        eventPublisher.publishEvent(new JobApplicationCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getResumeId(),
                saved.getCompanyName(),
                saved.getRoleTitle(),
                saved.getJobDescription()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<JobApplication> listApplicationsForUser(UUID userId, ApplicationStatus statusFilter) {
        if (statusFilter != null) {
            return repository.findByUserIdAndStatusOrderByAppliedAtDesc(userId, statusFilter);
        }
        return repository.findByUserIdOrderByAppliedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public JobApplication getApplicationForUser(UUID userId, UUID applicationId) {
        JobApplication app = repository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        app.getInterviewRounds().size();
        return app;
    }

    @Transactional
    public JobApplication updateStatusForUser(UUID userId, UUID applicationId, ApplicationStatus newStatus) {
        JobApplication app = repository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        app.transitionStatus(newStatus);
        return repository.save(app);
    }

    @Transactional
    public void deleteApplicationForUser(UUID userId, UUID applicationId) {
        JobApplication app = repository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        repository.delete(app);
    }

    @Transactional
    public InterviewRound scheduleInterviewForUser(
            UUID userId,
            UUID applicationId,
            String roundType,
            Instant scheduledAt,
            String notes
    ) {
        JobApplication app = repository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        InterviewRound round = app.scheduleInterviewRound(roundType, scheduledAt, notes);
        repository.save(app);
        return round;
    }

    @Transactional
    public InterviewRound updateInterviewOutcomeForUser(
            UUID userId,
            UUID applicationId,
            UUID roundId,
            String outcome,
            String questionsAsked
    ) {
        JobApplication app = repository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        InterviewRound round = app.recordInterviewOutcome(roundId, outcome, questionsAsked);
        repository.save(app);
        return round;
    }

    @Transactional(readOnly = true)
    public DashboardStats getStatsForUser(UUID userId) {
        List<JobApplication> all = repository.findByUserIdOrderByAppliedAtDesc(userId);
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            counts.put(s, 0L);
        }
        long progressedToInterviewOrOffer = 0;
        for (JobApplication app : all) {
            counts.merge(app.getStatus(), 1L, Long::sum);
            if (app.getStatus() == ApplicationStatus.INTERVIEWING
                    || app.getStatus() == ApplicationStatus.OFFER
                    || !app.getInterviewRounds().isEmpty()) {
                progressedToInterviewOrOffer++;
            }
        }
        double conversionRate = all.isEmpty()
                ? 0.0
                : Math.round((progressedToInterviewOrOffer * 1000.0) / all.size()) / 10.0;

        return new DashboardStats(all.size(), counts, conversionRate);
    }
}
