package com.jkingai.diagramarchitect.dto;

import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiagramRequest(
        @NotBlank(message = "The 'code' field is required and must not be blank.")
        @Size(max = 50000, message = "Code input exceeds the maximum allowed size of 50,000 characters.")
        String code,

        @NotNull(message = "The 'diagramType' field is required.")
        DiagramType diagramType,

        @NotNull(message = "The 'codeLanguage' field is required.")
        CodeLanguage codeLanguage,

        @Size(max = 500, message = "Context must not exceed 500 characters.")
        String context
) {
}
