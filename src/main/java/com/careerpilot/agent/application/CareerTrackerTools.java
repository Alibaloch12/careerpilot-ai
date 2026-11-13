package com.careerpilot.agent.application;

import com.careerpilot.tracker.application.JobApplicationService;
import com.careerpilot.tracker.domain.InterviewRound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class CareerTrackerTools {

    private static final Logger log = LoggerFactory.getLogger(CareerTrackerTools.class);

    private final JobApplicationService jobApplicationService;

    public CareerTrackerTools(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    public record ToolExecutionResult(String toolName, String summary, UUID resourceId) {}

    public ToolExecutionResult executeScheduleInterviewTool(
            UUID userId,
            UUID applicationId,
            String roundType,
            String preparationNotes
    ) {
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.DAYS);
        InterviewRound round = jobApplicationService.scheduleInterviewForUser(
                userId,
                applicationId,
                roundType,
                scheduledAt,
                preparationNotes
        );
        log.info("[Agent Tool Executed] scheduleInterviewTool created round #{} ({}) for applicationId={}",
                round.getRoundNumber(), roundType, applicationId);

        return new ToolExecutionResult(
                "scheduleInterviewRound",
                "Scheduled Round #" + round.getRoundNumber() + " (" + roundType + ") in tracker",
                round.getId()
        );
    }

    public String getPipelineSummaryTool(UUID userId) {
        JobApplicationService.DashboardStats stats = jobApplicationService.getStatsForUser(userId);
        return "Total Applications: " + stats.totalApplications()
                + ", Conversion Rate: " + stats.interviewConversionRatePercent() + "%"
                + ", Breakdown: " + stats.byStatus();
    }
}
