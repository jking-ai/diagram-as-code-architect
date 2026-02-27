package com.jkingai.diagramarchitect.exception;

public class LlmRateLimitException extends DiagramGenerationException {

    private final int retryAfterSeconds;

    public LlmRateLimitException(String message, Throwable cause, int retryAfterSeconds) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
