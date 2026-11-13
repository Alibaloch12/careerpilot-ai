package com.careerpilot.agent.application;

import com.careerpilot.shared.event.JobApplicationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MatchAssessmentListener {

    private static final Logger log = LoggerFactory.getLogger(MatchAssessmentListener.class);

    private final MatchAssessmentService matchAssessmentService;

    public MatchAssessmentListener(MatchAssessmentService matchAssessmentService) {
        this.matchAssessmentService = matchAssessmentService;
    }

    @EventListener
    public void onJobApplicationCreated(JobApplicationCreatedEvent event) {
        if (event.resumeId() == null || event.jobDescription() == null) {
            return;
        }
        log.info("[Agent Listener] Triggering match evaluation for applicationId={}", event.applicationId());
        matchAssessmentService.evaluateApplicationMatch(
                event.userId(),
                event.applicationId(),
                event.resumeId(),
                event.jobDescription()
        );
    }
}
