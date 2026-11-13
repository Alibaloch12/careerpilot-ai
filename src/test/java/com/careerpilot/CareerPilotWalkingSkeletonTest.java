package com.careerpilot;

import com.careerpilot.agent.application.MatchAssessmentService;
import com.careerpilot.agent.domain.AiAssessment;
import com.careerpilot.identity.application.AuthService;
import com.careerpilot.resume.application.ResumeService;
import com.careerpilot.resume.domain.EmbeddingStatus;
import com.careerpilot.resume.domain.Resume;
import com.careerpilot.tracker.application.JobApplicationService;
import com.careerpilot.tracker.domain.ApplicationStatus;
import com.careerpilot.tracker.domain.JobApplication;
import com.careerpilot.tracker.domain.WorkMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CareerPilotWalkingSkeletonTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private MatchAssessmentService matchAssessmentService;

    @Test
    void walkingSkeleton_flowsFromIdentityToResumeToTrackerToRagAndAgent() {
        // 1. Identity Module: Register a candidate with BCrypt & JWT
        AuthService.AuthResult auth = authService.register(
                "murad@careerpilot.ai",
                "StrongPassword123!",
                "Murad Baloch"
        );
        assertThat(auth.userId()).isNotNull();
        assertThat(auth.accessToken()).isNotBlank();

        // 2. Resume Module -> Fires ResumeUploadedEvent -> RAG Module chunks & persists vectors in DB
        Resume resume = resumeService.uploadResume(
                auth.userId(),
                "Java Backend & AI Resume v1",
                "Experienced in Java, Spring Boot, and PostgreSQL.\n\nBuilt REST APIs and microservices."
        );
        Resume updatedResume = resumeService.getResume(resume.getId());
        assertThat(updatedResume.getEmbeddingStatus()).isEqualTo(EmbeddingStatus.COMPLETED);

        // 3. Tracker Module -> Fires JobApplicationCreatedEvent -> Agent Module queries RAG & saves AI Assessment
        JobApplication application = jobApplicationService.createApplication(
                auth.userId(),
                resume.getId(),
                "Stripe",
                "Backend AI Engineer",
                WorkMode.REMOTE,
                120000,
                150000,
                "USD",
                "Looking for Java, Spring Boot, PostgreSQL, Docker, and AI skills."
        );
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);

        // 4. Verify Agent Module automatically created an AI Assessment using the persisted RAG chunks
        List<AiAssessment> assessments = matchAssessmentService.getAssessmentsForApplication(application.getId());
        assertThat(assessments).hasSize(1);
        AiAssessment assessment = assessments.getFirst();
        assertThat(assessment.getMatchScore()).isEqualTo(60); // Matched 3 of 5 JD skills: java, spring boot, postgresql
        assertThat(assessment.getMatchingSkills()).contains("java", "spring boot", "postgresql");
        assertThat(assessment.getMissingSkills()).contains("docker", "ai");
    }
}
