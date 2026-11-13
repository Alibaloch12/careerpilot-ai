package com.careerpilot.agent.application;

import com.careerpilot.rag.application.ResumeRagService;
import com.careerpilot.tracker.application.JobApplicationService;
import com.careerpilot.tracker.domain.JobApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InterviewCoachService {

    private final JobApplicationService jobApplicationService;
    private final ResumeRagService resumeRagService;
    private final MatchAssessmentService matchAssessmentService;
    private final CareerTrackerTools trackerTools;
    private final LlmGateway llmGateway;

    public InterviewCoachService(
            JobApplicationService jobApplicationService,
            ResumeRagService resumeRagService,
            MatchAssessmentService matchAssessmentService,
            CareerTrackerTools trackerTools,
            LlmGateway llmGateway
    ) {
        this.jobApplicationService = jobApplicationService;
        this.resumeRagService = resumeRagService;
        this.matchAssessmentService = matchAssessmentService;
        this.trackerTools = trackerTools;
        this.llmGateway = llmGateway;
    }

    public record InterviewPrepPlan(
            UUID applicationId,
            String companyName,
            String roleTitle,
            List<String> likelyTechnicalQuestions,
            List<String> systemDesignTopics,
            String coachingAdvice,
            String modelUsed,
            CareerTrackerTools.ToolExecutionResult scheduledRoundAction
    ) {}

    public record AgentChatResponse(
            String reply,
            String modelUsed,
            String pipelineContextUsed,
            List<CareerTrackerTools.ToolExecutionResult> executedTools
    ) {}

    @Transactional
    public InterviewPrepPlan generateInterviewPrep(
            UUID userId,
            UUID applicationId,
            String roundType,
            boolean autoScheduleInTracker
    ) {
        JobApplication app = jobApplicationService.getApplicationForUser(userId, applicationId);
        List<String> resumeChunks = app.getResumeId() != null
                ? resumeRagService.retrieveRelevantChunks(userId, app.getResumeId())
                : List.of();

        String jdText = app.getJobDescription() != null ? app.getJobDescription() : "";
        List<String> detectedSkills = matchAssessmentService.extractSkillsFromText(jdText.toLowerCase(Locale.ROOT));
        String primarySkills = detectedSkills.isEmpty()
                ? "Java, Spring Boot, and Backend Architecture"
                : String.join(", ", detectedSkills);

        String prompt = """
                Prepare candidate for a %s interview at %s for role %s.
                Target Skills in JD: %s
                Job Description: %s
                Candidate Resume Chunks: %s
                """.formatted(
                roundType,
                app.getCompanyName(),
                app.getRoleTitle(),
                primarySkills,
                jdText,
                String.join("\n", resumeChunks)
        );

        LlmGateway.LlmResponse llmResponse = llmGateway.generate(
                "You are a Staff Engineer conducting a technical and system design interview for " + app.getCompanyName() + ".",
                prompt
        );

        List<String> technicalQuestions = buildDynamicTechnicalQuestions(app.getCompanyName(), app.getRoleTitle(), detectedSkills);
        List<String> systemDesignTopics = buildDynamicSystemDesignTopics(app.getCompanyName(), app.getRoleTitle(), detectedSkills);

        CareerTrackerTools.ToolExecutionResult toolAction = null;
        if (autoScheduleInTracker) {
            toolAction = trackerTools.executeScheduleInterviewTool(
                    userId,
                    applicationId,
                    roundType,
                    "Target skills to review for " + app.getCompanyName() + ": " + primarySkills
            );
        }

        return new InterviewPrepPlan(
                app.getId(),
                app.getCompanyName(),
                app.getRoleTitle(),
                technicalQuestions,
                systemDesignTopics,
                llmResponse.content(),
                llmResponse.modelUsed(),
                toolAction
        );
    }

    private List<String> buildDynamicTechnicalQuestions(String company, String role, List<String> skills) {
        List<String> questions = new ArrayList<>();
        if (skills.contains("spring boot") || skills.contains("java")) {
            questions.add("How do you design transactional boundaries (@Transactional) and optimistic locking (@Version) in Spring Boot for high-concurrency workloads at " + company + "?");
        }
        if (skills.contains("postgresql") || skills.contains("sql") || skills.contains("pgvector")) {
            questions.add("How do you optimize PostgreSQL indexing, connection pooling (HikariCP), and query execution plans for the " + role + " role?");
        }
        if (skills.contains("ai") || skills.contains("rag") || skills.contains("llm") || skills.contains("vector")) {
            questions.add("How do you prevent cross-tenant data leakage and reduce hallucination latency in a production RAG / LLM pipeline?");
        }
        if (skills.contains("docker") || skills.contains("kubernetes") || skills.contains("aws") || skills.contains("microservices")) {
            questions.add("How do you handle container health checks, zero-downtime deployments, and failure isolation across distributed services?");
        }
        while (questions.size() < 3) {
            questions.add("Walk through the most complex production bug or architectural trade-off you resolved that directly relates to " + role + " at " + company + ".");
        }
        return questions.subList(0, 3);
    }

    private List<String> buildDynamicSystemDesignTopics(String company, String role, List<String> skills) {
        List<String> topics = new ArrayList<>();
        topics.add("Designing a scalable, fault-tolerant backend architecture for " + company + " (" + role + ") with strict SLA & observability guarantees");
        if (skills.contains("ai") || skills.contains("rag") || skills.contains("llm")) {
            topics.add("Asynchronous event-driven AI ingestion & semantic retrieval pipeline with rate-limiting and fallback circuit breakers");
        } else {
            topics.add("High-throughput API gateway, distributed caching (Redis), and database partitioning strategy");
        }
        return topics;
    }

    @Transactional
    public AgentChatResponse chatWithAgent(UUID userId, String message, UUID applicationId) {
        String pipelineSummary = trackerTools.getPipelineSummaryTool(userId);
        List<CareerTrackerTools.ToolExecutionResult> executedTools = new ArrayList<>();

        String appContext = "No specific application selected.";
        if (applicationId != null) {
            JobApplication app = jobApplicationService.getApplicationForUser(userId, applicationId);
            appContext = "Company: " + app.getCompanyName()
                    + ", Role: " + app.getRoleTitle()
                    + ", Status: " + app.getStatus()
                    + ", JD: " + (app.getJobDescription() != null ? app.getJobDescription() : "N/A");

            String lowerMsg = message.toLowerCase(Locale.ROOT);
            if (lowerMsg.contains("schedule") || lowerMsg.contains("book")) {
                String detectedRoundType = lowerMsg.contains("system")
                        ? "SYSTEM_DESIGN"
                        : lowerMsg.contains("behavioral") ? "BEHAVIORAL" : "TECHNICAL";

                CareerTrackerTools.ToolExecutionResult action = trackerTools.executeScheduleInterviewTool(
                        userId,
                        applicationId,
                        detectedRoundType,
                        "Scheduled via AI Coach: " + message
                );
                executedTools.add(action);
            }
        }

        String groundedPrompt = """
                User Request: %s
                Selected Application Context: %s
                Live Pipeline Metrics: %s
                Tools Executed: %s
                """.formatted(message, appContext, pipelineSummary, executedTools);

        LlmGateway.LlmResponse llmResponse = llmGateway.generate(
                "You are CareerPilot AI, an intelligent career engineering assistant connected to the user's live job tracker.",
                groundedPrompt
        );

        return new AgentChatResponse(
                llmResponse.content(),
                llmResponse.modelUsed(),
                pipelineSummary,
                executedTools
        );
    }
}
