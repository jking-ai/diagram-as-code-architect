package com.jkingai.diagramarchitect.controller;

import com.jkingai.diagramarchitect.dto.ErrorResponse;
import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import com.jkingai.diagramarchitect.exception.LlmRateLimitException;
import com.jkingai.diagramarchitect.exception.LlmServiceUnavailableException;
import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Validation failed.");
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", message, Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Invalid request body. Check enum values for diagramType and codeLanguage.";
        if (ex.getMessage() != null && ex.getMessage().contains("DiagramType")) {
            message = "Invalid diagram type. Valid values: FLOWCHART, SEQUENCE, CLASS, ENTITY_RELATIONSHIP, INFRASTRUCTURE.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("CodeLanguage")) {
            message = "Invalid code language. Valid values: JAVA, HCL.";
        }
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_ERROR", message, Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String errorCode = ex.getMessage() != null && ex.getMessage().contains("50,000")
                ? "CODE_TOO_LARGE" : "VALIDATION_ERROR";
        return ResponseEntity.badRequest().body(
                new ErrorResponse(errorCode, ex.getMessage(), Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(UnsupportedDiagramTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedType(UnsupportedDiagramTypeException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("UNSUPPORTED_DIAGRAM_TYPE", ex.getMessage(), Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(LlmRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(LlmRateLimitException ex, HttpServletRequest request) {
        log.warn("LLM rate limited: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ErrorResponse("RATE_LIMITED", ex.getMessage(), Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(LlmServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(LlmServiceUnavailableException ex, HttpServletRequest request) {
        log.error("LLM service unavailable (circuit breaker open): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ErrorResponse("SERVICE_UNAVAILABLE", ex.getMessage(), Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(DiagramGenerationException.class)
    public ResponseEntity<ErrorResponse> handleGenerationFailed(DiagramGenerationException ex, HttpServletRequest request) {
        log.error("Diagram generation failed: {}", ex.getMessage(), ex);
        HttpStatus status = (ex.getCause() != null) ? HttpStatus.BAD_GATEWAY : HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = (ex.getCause() != null) ? "LLM_ERROR" : "GENERATION_FAILED";
        return ResponseEntity.status(status).body(
                new ErrorResponse(errorCode, ex.getMessage(), Instant.now(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred.", Instant.now(), request.getRequestURI()));
    }
}
