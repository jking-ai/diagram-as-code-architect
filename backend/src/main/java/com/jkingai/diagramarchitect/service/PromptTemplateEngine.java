package com.jkingai.diagramarchitect.service;

import com.jkingai.diagramarchitect.exception.UnsupportedDiagramTypeException;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptTemplateEngine {

    private static final String TEMPLATE_PATH = "prompt/templates/";
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String assemblePrompt(CodeLanguage language, DiagramType diagramType, String code, String context) {
        if (!diagramType.supportsLanguage(language)) {
            throw new UnsupportedDiagramTypeException(
                    String.format("Diagram type %s is not supported for code language %s. Supported types for %s: %s.",
                            diagramType, language, language, getSupportedTypesForLanguage(language)));
        }

        String template = loadTemplate(language, diagramType);
        String assembled = template.replace("{code}", code);
        assembled = assembled.replace("{context}",
                (context != null && !context.isBlank()) ? context : "No additional context provided.");
        return assembled;
    }

    private String loadTemplate(CodeLanguage language, DiagramType diagramType) {
        String key = templateKey(language, diagramType);
        return templateCache.computeIfAbsent(key, k -> {
            String filename = TEMPLATE_PATH + k + ".txt";
            try {
                ClassPathResource resource = new ClassPathResource(filename);
                return resource.getContentAsString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load prompt template: " + filename, e);
            }
        });
    }

    private String templateKey(CodeLanguage language, DiagramType diagramType) {
        String lang = language.name().toLowerCase();
        String type = diagramType.name().toLowerCase().replace("_", "-");
        return lang + "-" + type;
    }

    private String getSupportedTypesForLanguage(CodeLanguage language) {
        StringBuilder sb = new StringBuilder();
        for (DiagramType dt : DiagramType.values()) {
            if (dt.supportsLanguage(language)) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(dt.name());
            }
        }
        return sb.toString();
    }
}
