package com.careerpilot.agent.application;

public interface LlmGateway {

    record LlmResponse(String content, String modelUsed) {}

    LlmResponse generate(String systemInstruction, String userPrompt);
}
