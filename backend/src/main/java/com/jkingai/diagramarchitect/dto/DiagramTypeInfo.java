package com.jkingai.diagramarchitect.dto;

import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;

import java.util.List;

public record DiagramTypeInfo(
        DiagramType type,
        String name,
        String description,
        List<CodeLanguage> supportedLanguages,
        String mermaidDirective
) {
}
