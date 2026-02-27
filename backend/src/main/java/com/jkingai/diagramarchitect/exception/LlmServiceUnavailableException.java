package com.jkingai.diagramarchitect.exception;

public class LlmServiceUnavailableException extends DiagramGenerationException {

    private final int retryAfterSeconds;

    public LlmServiceUnavailableException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
