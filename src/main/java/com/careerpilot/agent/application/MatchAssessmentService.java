package com.careerpilot.agent.application;

import com.careerpilot.agent.domain.AiAssessment;
import com.careerpilot.agent.domain.AiAssessmentRepository;
import com.careerpilot.rag.application.ResumeRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MatchAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(MatchAssessmentService.class);

    private static final List<String> TECH_SKILL_CATALOG = List.of(
            "java", "spring boot", "spring security", "spring ai", "spring data", "hibernate", "jpa",
            "postgresql", "mysql", "mongodb", "redis", "kafka", "rabbitmq", "elasticsearch",
            "docker", "kubernetes", "aws", "gcp", "azure", "terraform", "ci/cd", "github actions",
            "jenkins", "linux", "git", "maven", "gradle", "rest", "graphql", "grpc", "microservices",
            "python", "typescript", "javascript", "react", "angular", "vue", "next.js", "node.js",
            "go", "golang", "rust", "c++", "c#", "sql", "nosql", "flyway", "liquibase",
            "junit", "mockito", "testcontainers", "rag", "llm", "langchain", "langchain4j",
            "openai", "gemini", "ollama", "pgvector", "vector", "machine learning", "ai",
            "system design", "distributed systems", "concurrency", "multithreading", "oauth2", "jwt"
    );

    private final ResumeRagService resumeRagService;
    private final AiAssessmentRepository assessmentRepository;
    private final LlmGateway llmGateway;

    public MatchAssessmentService(
            ResumeRagService resumeRagService,
            AiAssessmentRepository assessmentRepository,
            LlmGateway llmGateway
    ) {
        this.resumeRagService = resumeRagService;
        this.assessmentRepository = assessmentRepository;
        this.llmGateway = llmGateway;
    }

    @Transactional
    public AiAssessment evaluateApplicationMatch(
            UUID userId,
            UUID applicationId,
            UUID resumeId,
            String jobDescription
    ) {
        List<String> resumeChunks = resumeRagService.retrieveRelevantChunks(userId, resumeId);
        String combinedResume = String.join(" ", resumeChunks).toLowerCase(Locale.ROOT);
        String normalizedJd = (jobDescription != null ? jobDescription : "").toLowerCase(Locale.ROOT);

        // Dynamically extract required skills present in the Job Description
        List<String> jdRequiredSkills = extractSkillsFromText(normalizedJd);
        if (jdRequiredSkills.isEmpty()) {
            jdRequiredSkills = List.of("java", "spring boot", "postgresql", "docker", "ai");
        }

        List<String> matched = jdRequiredSkills.stream()
                .filter(combinedResume::contains)
                .toList();
        List<String> missing = jdRequiredSkills.stream()
                .filter(k -> !combinedResume.contains(k))
                .toList();

        int score = (int) Math.round((matched.size() * 100.0) / jdRequiredSkills.size());

        String prompt = """
                Job Description: %s
                Candidate Resume Content: %s
                Matched Skills (%d/%d): %s
                Missing Skills: %s
                Provide actionable resume tailoring recommendations for this candidate.
                """.formatted(
                jobDescription,
                String.join("\n", resumeChunks),
                matched.size(),
                jdRequiredSkills.size(),
                matched,
                missing
        );

        LlmGateway.LlmResponse llmResponse = llmGateway.generate(
                "You are an expert technical recruiter and Principal Backend & AI Architect.",
                prompt
        );

        String suggestions = missing.isEmpty()
                ? "Strong match across all extracted job requirements (" + String.join(", ", matched) + "). " + llmResponse.content()
                : "To increase your match rate from " + score + "% to 90%+, add concrete bullet points demonstrating: "
                  + String.join(", ", missing) + ". " + llmResponse.content();

        AiAssessment assessment = new AiAssessment(
                userId,
                applicationId,
                resumeId,
                score,
                String.join(", ", matched),
                String.join(", ", missing),
                suggestions,
                llmResponse.modelUsed()
        );
        AiAssessment saved = assessmentRepository.save(assessment);
        log.info("[Agent Module] Saved AI Assessment score={}% (matched={}, missing={}) for applicationId={}",
                score, matched, missing, applicationId);
        return saved;
    }

    public List<String> extractSkillsFromText(String lowerText) {
        if (lowerText == null || lowerText.isBlank()) {
            return List.of();
        }
        List<String> detected = new ArrayList<>();
        for (String skill : TECH_SKILL_CATALOG) {
            if (containsSkillToken(lowerText, skill)) {
                detected.add(skill);
            }
        }
        return detected;
    }

    private boolean containsSkillToken(String text, String skill) {
        int idx = text.indexOf(skill);
        while (idx != -1) {
            boolean leftBoundary = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1));
            int endIdx = idx + skill.length();
            boolean rightBoundary = endIdx >= text.length() || !Character.isLetterOrDigit(text.charAt(endIdx));
            if (leftBoundary && rightBoundary) {
                return true;
            }
            idx = text.indexOf(skill, idx + 1);
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<AiAssessment> getAssessmentsForApplication(UUID applicationId) {
        return assessmentRepository.findByApplicationId(applicationId);
    }
}
