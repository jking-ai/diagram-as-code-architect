package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.config.AiConfig;
import com.jkingai.diagramarchitect.dto.DiagramRequest;
import com.jkingai.diagramarchitect.dto.DiagramResponse;
import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import com.jkingai.diagramarchitect.exception.LlmRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiagramGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DiagramGenerationService.class);

    private final CodeAnalysisService codeAnalysisService;
    private final PromptTemplateEngine promptTemplateEngine;
    private final MermaidSyntaxExtractor mermaidSyntaxExtractor;
    private final ResilientLlmClient resilientLlmClient;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-2.0-flash}")
    private String modelName;

    public DiagramGenerationService(CodeAnalysisService codeAnalysisService,
                                     PromptTemplateEngine promptTemplateEngine,
                                     MermaidSyntaxExtractor mermaidSyntaxExtractor,
                                     ResilientLlmClient resilientLlmClient) {
        this.codeAnalysisService = codeAnalysisService;
        this.promptTemplateEngine = promptTemplateEngine;
        this.mermaidSyntaxExtractor = mermaidSyntaxExtractor;
        this.resilientLlmClient = resilientLlmClient;
    }

    public DiagramResponse generate(DiagramRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Validate and normalize
        String normalizedCode = codeAnalysisService.validateAndNormalize(
                request.code(), request.diagramType(), request.codeLanguage());

        // Step 2: Assemble the prompt
        String prompt = promptTemplateEngine.assemblePrompt(
                request.codeLanguage(), request.diagramType(), normalizedCode, request.context());

        // Step 3: Call the LLM and extract Mermaid syntax
        String mermaidSyntax;
        try {
            log.info("Sending prompt to {} for {}/{} ({} chars)",
                    modelName, request.codeLanguage(), request.diagramType(), normalizedCode.length());

            String llmResponse = resilientLlmClient.call(prompt);

            if (llmResponse == null || llmResponse.isBlank()) {
                throw new DiagramGenerationException("The AI service returned an empty response. Please try again.");
            }

            // Step 4: Extract Mermaid syntax (inside try so extraction errors are caught)
            mermaidSyntax = mermaidSyntaxExtractor.extract(llmResponse);
        } catch (LlmRateLimitException e) {
            throw e; // already the right type
        } catch (DiagramGenerationException e) {
            throw e; // already the right type
        } catch (Exception e) {
            log.error("LLM call failed for {}/{}: {}", request.codeLanguage(), request.diagramType(), e.getMessage(), e);

            if (AiConfig.isRateLimitError(e)) {
                throw new LlmRateLimitException(
                        "The AI service is currently experiencing high demand. Please wait a moment and try again.",
                        e, 30);
            }

            throw new DiagramGenerationException("The upstream AI service failed to generate a diagram. Please try again.", e);
        }

        long processingTimeMs = System.currentTimeMillis() - startTime;
        log.info("Generated {} diagram for {} in {}ms", request.diagramType(), request.codeLanguage(), processingTimeMs);

        // Step 5: Build response
        return new DiagramResponse(
                mermaidSyntax,
                request.diagramType(),
                request.codeLanguage(),
                Map.of(
                        "model", modelName,
                        "inputCharacters", normalizedCode.length(),
                        "processingTimeMs", processingTimeMs
                )
        );
    }
}
