package com.careerpilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Milestone1SecurityAndTrackerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullSecurityAndTrackerFlow_registersLogsInTracksJobsSchedulesInterviewsAndComputesStats() throws Exception {
        // 1. Unauthenticated access to protected endpoint must return 401 Unauthorized
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());

        // 2. Register a new candidate via /api/v1/auth/register
        String registerPayload = """
                {
                  "email": "candidate@careerpilot.ai",
                  "password": "StrongPassword123!",
                  "fullName": "Murad Baloch"
                }
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("candidate@careerpilot.ai"))
                .andReturn();

        JsonNode authNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String jwtToken = authNode.get("accessToken").asText();

        // 3. Login with the same credentials via /api/v1/auth/login
        String loginPayload = """
                {
                  "email": "candidate@careerpilot.ai",
                  "password": "StrongPassword123!"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 4. Upload a Resume using Bearer JWT (no userId needed in request body!)
        String resumePayload = """
                {
                  "versionName": "Backend + AI Resume v1",
                  "rawContent": "Experienced in Java, Spring Boot, PostgreSQL, Docker, and AI systems."
                }
                """;
        MvcResult resumeResult = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionName").value("Backend + AI Resume v1"))
                .andReturn();

        String resumeId = objectMapper.readTree(resumeResult.getResponse().getContentAsString())
                .get("id").asText();

        // 5. Create a Job Application linked to the Resume
        String applicationPayload = """
                {
                  "resumeId": "%s",
                  "companyName": "Datadog",
                  "roleTitle": "Software Engineer - AI Platform",
                  "workMode": "HYBRID",
                  "salaryMin": 130000,
                  "salaryMax": 160000,
                  "currency": "EUR",
                  "jobDescription": "Build scalable Java, Spring Boot, PostgreSQL, Docker, and AI services."
                }
                """.formatted(resumeId);

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Datadog"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andReturn();

        String applicationId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .get("id").asText();

        // 6. Schedule a Technical Interview Round -> Automatically transitions status to INTERVIEWING
        String interviewPayload = """
                {
                  "roundType": "SYSTEM_DESIGN",
                  "scheduledAt": "2026-10-25T14:00:00Z",
                  "preparationNotes": "Review Modular Monolith, pgvector, and Spring AI event flows"
                }
                """;
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/interviews")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(interviewPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roundNumber").value(1))
                .andExpect(jsonPath("$.roundType").value("SYSTEM_DESIGN"));

        // 7. Filter applications by status=INTERVIEWING
        mockMvc.perform(get("/api/v1/applications")
                        .param("status", "INTERVIEWING")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Datadog"));

        // 8. Transition application status to OFFER
        mockMvc.perform(patch("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OFFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFER"));

        // 9. Verify Dashboard Stats (/api/v1/applications/stats)
        MvcResult statsResult = mockMvc.perform(get("/api/v1/applications/stats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(1))
                .andExpect(jsonPath("$.byStatus.OFFER").value(1))
                .andExpect(jsonPath("$.interviewConversionRatePercent").value(100.0))
                .andReturn();

        assertThat(statsResult.getResponse().getContentAsString()).contains("\"interviewConversionRatePercent\":100.0");

        // 10. Verify AI Assessment was also generated automatically with 100% match score!
        mockMvc.perform(get("/api/v1/agent/assessments/" + applicationId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchScore").value(100));
    }
}
