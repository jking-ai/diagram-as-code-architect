package com.jkingai.diagramarchitect.dto;

import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;

import java.util.Map;

public record DiagramResponse(
        String mermaidSyntax,
        DiagramType diagramType,
        CodeLanguage codeLanguage,
        Map<String, Object> metadata
) {
}
