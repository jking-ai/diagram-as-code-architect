package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.dto.DiagramRequest;
import com.jkingai.diagramarchitect.dto.DiagramResponse;
import com.jkingai.diagramarchitect.exception.DiagramGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiagramGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DiagramGenerationService.class);

    private final CodeAnalysisService codeAnalysisService;
    private final PromptTemplateEngine promptTemplateEngine;
    private final MermaidSyntaxExtractor mermaidSyntaxExtractor;
    private final ChatClient chatClient;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-2.0-flash}")
    private String modelName;

    public DiagramGenerationService(CodeAnalysisService codeAnalysisService,
                                     PromptTemplateEngine promptTemplateEngine,
                                     MermaidSyntaxExtractor mermaidSyntaxExtractor,
                                     ChatClient chatClient) {
        this.codeAnalysisService = codeAnalysisService;
        this.promptTemplateEngine = promptTemplateEngine;
        this.mermaidSyntaxExtractor = mermaidSyntaxExtractor;
        this.chatClient = chatClient;
    }

    public DiagramResponse generate(DiagramRequest request) {
        long startTime = System.currentTimeMillis();

        // Step 1: Validate and normalize
        String normalizedCode = codeAnalysisService.validateAndNormalize(
                request.code(), request.diagramType(), request.codeLanguage());

        // Step 2: Assemble the prompt
        String prompt = promptTemplateEngine.assemblePrompt(
                request.codeLanguage(), request.diagramType(), normalizedCode, request.context());

        // Step 3: Call the LLM
        String llmResponse;
        try {
            log.info("Sending prompt to {} for {}/{} ({} chars)",
                    modelName, request.codeLanguage(), request.diagramType(), normalizedCode.length());

            llmResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("LLM call failed for {}/{}: {}", request.codeLanguage(), request.diagramType(), e.getMessage(), e);
            throw new DiagramGenerationException("The upstream AI service failed to generate a diagram. Please try again.", e);
        }

        // Step 4: Extract Mermaid syntax
        String mermaidSyntax = mermaidSyntaxExtractor.extract(llmResponse);

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
