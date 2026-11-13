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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Milestone3AiAgentAndCoachTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aiAgent_generatesInterviewPrepAndExecutesToolCallsToScheduleInterviewRounds() throws Exception {
        // 1. Register user and obtain Bearer JWT
        String registerPayload = """
                {
                  "email": "agent-test@careerpilot.ai",
                  "password": "StrongPassword123!",
                  "fullName": "Murad Baloch"
                }
                """;
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String jwtToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 2. Upload Resume
        String resumePayload = """
                {
                  "versionName": "AI Backend Resume v2",
                  "rawContent": "Built Modular Monolith in Java 21, Spring Boot 3, PostgreSQL pgvector, and Spring AI."
                }
                """;
        MvcResult resumeResult = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resumePayload))
                .andExpect(status().isCreated())
                .andReturn();

        String resumeId = objectMapper.readTree(resumeResult.getResponse().getContentAsString())
                .get("id").asText();

        // 3. Create Job Application
        String appPayload = """
                {
                  "resumeId": "%s",
                  "companyName": "OpenAI",
                  "roleTitle": "Backend Software Engineer",
                  "workMode": "REMOTE",
                  "salaryMin": 160000,
                  "salaryMax": 200000,
                  "currency": "USD",
                  "jobDescription": "Looking for Java, Spring Boot, PostgreSQL, and AI agent architecture experience."
                }
                """.formatted(resumeId);

        MvcResult appResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String applicationId = objectMapper.readTree(appResult.getResponse().getContentAsString())
                .get("id").asText();

        // 4. Generate AI Interview Prep with autoScheduleInTracker=true
        String prepPayload = """
                {
                  "roundType": "TECHNICAL",
                  "autoScheduleInTracker": true
                }
                """;
        mockMvc.perform(post("/api/v1/agent/interview-prep/" + applicationId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prepPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("OpenAI"))
                .andExpect(jsonPath("$.likelyTechnicalQuestions.length()").value(3))
                .andExpect(jsonPath("$.scheduledRoundAction.toolName").value("scheduleInterviewRound"));

        // 5. Chat with AI Career Coach asking it to schedule a System Design round via tool-calling
        String chatPayload = """
                {
                  "message": "Please schedule a system design interview round for my OpenAI application and summarize my pipeline.",
                  "applicationId": "%s"
                }
                """.formatted(applicationId);

        mockMvc.perform(post("/api/v1/agent/chat")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty())
                .andExpect(jsonPath("$.executedTools.length()").value(1))
                .andExpect(jsonPath("$.executedTools[0].toolName").value("scheduleInterviewRound"));

        // 6. Verify the application in Tracker now has 2 scheduled interview rounds and status=INTERVIEWING!
        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEWING"))
                .andExpect(jsonPath("$.interviewRounds.length()").value(2))
                .andExpect(jsonPath("$.interviewRounds[0].roundType").value("TECHNICAL"))
                .andExpect(jsonPath("$.interviewRounds[1].roundType").value("SYSTEM_DESIGN"));
    }
}
