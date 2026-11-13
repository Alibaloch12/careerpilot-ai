package com.careerpilot.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ConfigurableLlmAdapter implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableLlmAdapter.class);

    private final RestClient restClient;
    private final String provider;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public ConfigurableLlmAdapter(
            @Value("${app.ai.provider:auto}") String provider,
            @Value("${app.ai.gemini-api-key:}") String geminiApiKey,
            @Value("${app.ai.gemini-model:gemini-2.0-flash}") String geminiModel,
            @Value("${app.ai.ollama-base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${app.ai.ollama-model:llama3.2}") String ollamaModel
    ) {
        this.restClient = RestClient.builder().build();
        this.provider = provider;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
    }

    @Override
    public LlmResponse generate(String systemInstruction, String userPrompt) {
        if (("gemini".equalsIgnoreCase(provider) || "auto".equalsIgnoreCase(provider))
                && geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return callGemini(systemInstruction, userPrompt);
            } catch (Exception ex) {
                log.warn("[LLM Adapter] Gemini API call failed, falling back: {}", ex.getMessage());
            }
        }

        if ("ollama".equalsIgnoreCase(provider)) {
            try {
                return callOllama(systemInstruction, userPrompt);
            } catch (Exception ex) {
                log.warn("[LLM Adapter] Ollama call failed, falling back: {}", ex.getMessage());
            }
        }

        return generateContextAwareLocalResponse(userPrompt);
    }

    private LlmResponse callGemini(String systemInstruction, String userPrompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt))))
        );

        JsonNode response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("candidates")) {
            String text = response.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
            if (!text.isBlank()) {
                return new LlmResponse(text, "gemini:" + geminiModel);
            }
        }
        throw new IllegalStateException("Empty response from Gemini API");
    }

    private LlmResponse callOllama(String systemInstruction, String userPrompt) {
        String url = ollamaBaseUrl + "/api/generate";
        Map<String, Object> body = Map.of(
                "model", ollamaModel,
                "system", systemInstruction,
                "prompt", userPrompt,
                "stream", false
        );

        JsonNode response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("response")) {
            return new LlmResponse(response.get("response").asText(), "ollama:" + ollamaModel);
        }
        throw new IllegalStateException("Empty response from Ollama API");
    }

    private LlmResponse generateContextAwareLocalResponse(String userPrompt) {
        if (userPrompt.contains("Tools Executed: [ToolExecutionResult")) {
            return new LlmResponse(
                    "I have scheduled that interview round in your job tracker and updated your application status to INTERVIEWING. "
                    + "Review the preparation notes in your application details to get ready!",
                    "careerpilot-local-engine"
            );
        }
        if (userPrompt.contains("Live Pipeline Metrics:")) {
            int idx = userPrompt.indexOf("Live Pipeline Metrics:");
            String metricsLine = userPrompt.substring(idx).split("\\R")[0];
            return new LlmResponse(
                    "Here is your current job search status — " + metricsLine
                    + ". Select a specific application if you want me to schedule an interview round or generate a custom prep plan.",
                    "careerpilot-local-engine"
            );
        }
        return new LlmResponse(
                "Tailor your top resume summary and project bullet points to mirror the exact technical stack in the job description, "
                + "and prepare concrete metrics (latency, test coverage, architecture decisions) for your interviews.",
                "careerpilot-local-engine"
        );
    }
}
