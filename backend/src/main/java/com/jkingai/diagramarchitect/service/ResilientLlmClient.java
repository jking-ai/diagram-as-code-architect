package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.config.AiConfig;
import com.jkingai.diagramarchitect.exception.LlmServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ResilientLlmClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientLlmClient.class);

    private final ChatClient chatClient;

    public ResilientLlmClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @CircuitBreaker(name = "gemini", fallbackMethod = "fallback")
    public String call(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @SuppressWarnings("unused")
    private String fallback(String prompt, Throwable t) {
        log.error("Circuit breaker open for Gemini — failing fast. Last error: {}", t.getMessage());
        throw new LlmServiceUnavailableException(
                "The AI service is temporarily unavailable due to repeated failures. Please try again in a few minutes.",
                30);
    }
}
