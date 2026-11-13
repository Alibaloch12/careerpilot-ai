package com.careerpilot.tracker.api;

import com.careerpilot.shared.security.AuthenticatedUser;
import com.careerpilot.tracker.application.JobApplicationService;
import com.careerpilot.tracker.domain.ApplicationStatus;
import com.careerpilot.tracker.domain.InterviewRound;
import com.careerpilot.tracker.domain.JobApplication;
import com.careerpilot.tracker.domain.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    public record CreateApplicationRequest(
            UUID resumeId,
            @NotBlank String companyName,
            @NotBlank String roleTitle,
            @NotNull WorkMode workMode,
            Integer salaryMin,
            Integer salaryMax,
            String currency,
            String jobDescription
    ) {}

    public record UpdateStatusRequest(@NotNull ApplicationStatus status) {}

    public record ScheduleInterviewRequest(
            @NotBlank String roundType,
            Instant scheduledAt,
            String preparationNotes
    ) {}

    public record UpdateInterviewOutcomeRequest(
            @NotBlank String outcome,
            String questionsAsked
    ) {}

    public record InterviewRoundResponse(
            UUID id,
            int roundNumber,
            String roundType,
            String outcome,
            String preparationNotes,
            String questionsAsked,
            Instant scheduledAt
    ) {
        public static InterviewRoundResponse from(InterviewRound r) {
            return new InterviewRoundResponse(
                    r.getId(),
                    r.getRoundNumber(),
                    r.getRoundType(),
                    r.getOutcome(),
                    r.getPreparationNotes(),
                    r.getQuestionsAsked(),
                    r.getScheduledAt()
            );
        }
    }

    public record ApplicationResponse(
            UUID id,
            UUID userId,
            UUID resumeId,
            String companyName,
            String roleTitle,
            WorkMode workMode,
            ApplicationStatus status,
            Integer salaryMin,
            Integer salaryMax,
            String currency,
            String jobDescription,
            Instant appliedAt,
            List<InterviewRoundResponse> interviewRounds
    ) {
        public static ApplicationResponse fromDomain(JobApplication app, boolean includeRounds) {
            List<InterviewRoundResponse> rounds = includeRounds
                    ? app.getInterviewRounds().stream().map(InterviewRoundResponse::from).toList()
                    : List.of();

            Integer min = app.getSalaryRange() != null ? app.getSalaryRange().getMin() : null;
            Integer max = app.getSalaryRange() != null ? app.getSalaryRange().getMax() : null;
            String curr = app.getSalaryRange() != null ? app.getSalaryRange().getCurrency() : null;

            return new ApplicationResponse(
                    app.getId(),
                    app.getUserId(),
                    app.getResumeId(),
                    app.getCompanyName(),
                    app.getRoleTitle(),
                    app.getWorkMode(),
                    app.getStatus(),
                    min,
                    max,
                    curr,
                    app.getJobDescription(),
                    app.getAppliedAt(),
                    rounds
            );
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        JobApplication app = service.createApplication(
                currentUser.userId(),
                request.resumeId(),
                request.companyName(),
                request.roleTitle(),
                request.workMode(),
                request.salaryMin(),
                request.salaryMax(),
                request.currency(),
                request.jobDescription()
        );
        return ApplicationResponse.fromDomain(app, false);
    }

    @GetMapping
    public List<ApplicationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) ApplicationStatus status
    ) {
        return service.listApplicationsForUser(currentUser.userId(), status)
                .stream()
                .map(app -> ApplicationResponse.fromDomain(app, false))
                .toList();
    }

    @GetMapping("/stats")
    public JobApplicationService.DashboardStats getStats(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return service.getStatsForUser(currentUser.userId());
    }

    @GetMapping("/{id}")
    public ApplicationResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id
    ) {
        JobApplication app = service.getApplicationForUser(currentUser.userId(), id);
        return ApplicationResponse.fromDomain(app, true);
    }

    @PatchMapping("/{id}/status")
    public ApplicationResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        JobApplication app = service.updateStatusForUser(currentUser.userId(), id, request.status());
        return ApplicationResponse.fromDomain(app, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id
    ) {
        service.deleteApplicationForUser(currentUser.userId(), id);
    }

    @PostMapping("/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewRoundResponse scheduleInterview(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleInterviewRequest request
    ) {
        InterviewRound round = service.scheduleInterviewForUser(
                currentUser.userId(),
                id,
                request.roundType(),
                request.scheduledAt(),
                request.preparationNotes()
        );
        return InterviewRoundResponse.from(round);
    }

    @PatchMapping("/{id}/interviews/{roundId}")
    public InterviewRoundResponse updateInterviewOutcome(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @PathVariable UUID roundId,
            @Valid @RequestBody UpdateInterviewOutcomeRequest request
    ) {
        InterviewRound round = service.updateInterviewOutcomeForUser(
                currentUser.userId(),
                id,
                roundId,
                request.outcome(),
                request.questionsAsked()
        );
        return InterviewRoundResponse.from(round);
    }
}
