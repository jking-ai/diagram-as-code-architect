package com.jkingai.diagramarchitect.dto;

import java.util.List;

public record DiagramTypeInfo(
        String type,
        String name,
        String description,
        List<String> supportedLanguages,
        String mermaidDirective
) {
}
