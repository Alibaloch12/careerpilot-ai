package com.careerpilot.agent.api;

import com.careerpilot.agent.application.InterviewCoachService;
import com.careerpilot.agent.application.MatchAssessmentService;
import com.careerpilot.agent.domain.AiAssessment;
import com.careerpilot.shared.security.AuthenticatedUser;
import com.careerpilot.tracker.application.JobApplicationService;
import com.careerpilot.tracker.domain.JobApplication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
public class CareerAgentController {

    private final MatchAssessmentService matchAssessmentService;
    private final InterviewCoachService interviewCoachService;
    private final JobApplicationService jobApplicationService;

    public CareerAgentController(
            MatchAssessmentService matchAssessmentService,
            InterviewCoachService interviewCoachService,
            JobApplicationService jobApplicationService
    ) {
        this.matchAssessmentService = matchAssessmentService;
        this.interviewCoachService = interviewCoachService;
        this.jobApplicationService = jobApplicationService;
    }

    public record AssessmentResponse(
            UUID id,
            UUID applicationId,
            UUID resumeId,
            int matchScore,
            String matchingSkills,
            String missingSkills,
            String tailoringSuggestions,
            String modelName
    ) {
        public static AssessmentResponse from(AiAssessment a) {
            return new AssessmentResponse(
                    a.getId(),
                    a.getApplicationId(),
                    a.getResumeId(),
                    a.getMatchScore(),
                    a.getMatchingSkills(),
                    a.getMissingSkills(),
                    a.getTailoringSuggestions(),
                    a.getModelName()
            );
        }
    }

    public record InterviewPrepRequest(
            @NotBlank String roundType,
            boolean autoScheduleInTracker
    ) {}

    public record AgentChatRequest(
            @NotBlank String message,
            UUID applicationId
    ) {}

    @GetMapping("/assessments/{applicationId}")
    public List<AssessmentResponse> getAssessments(@PathVariable UUID applicationId) {
        return matchAssessmentService.getAssessmentsForApplication(applicationId)
                .stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @PostMapping("/assessments/{applicationId}/evaluate")
    public AssessmentResponse evaluateOnDemand(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID applicationId,
            @RequestParam UUID resumeId
    ) {
        JobApplication app = jobApplicationService.getApplicationForUser(currentUser.userId(), applicationId);
        AiAssessment assessment = matchAssessmentService.evaluateApplicationMatch(
                currentUser.userId(),
                app.getId(),
                resumeId,
                app.getJobDescription() != null ? app.getJobDescription() : app.getRoleTitle()
        );
        return AssessmentResponse.from(assessment);
    }

    @PostMapping("/interview-prep/{applicationId}")
    public InterviewCoachService.InterviewPrepPlan generateInterviewPrep(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID applicationId,
            @Valid @RequestBody InterviewPrepRequest request
    ) {
        return interviewCoachService.generateInterviewPrep(
                currentUser.userId(),
                applicationId,
                request.roundType(),
                request.autoScheduleInTracker()
        );
    }

    @PostMapping("/chat")
    public InterviewCoachService.AgentChatResponse chat(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AgentChatRequest request
    ) {
        return interviewCoachService.chatWithAgent(
                currentUser.userId(),
                request.message(),
                request.applicationId()
        );
    }
}
