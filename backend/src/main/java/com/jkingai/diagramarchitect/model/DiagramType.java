package com.jkingai.diagramarchitect.model;

import java.util.Set;

public enum DiagramType {
    FLOWCHART(Set.of(CodeLanguage.JAVA, CodeLanguage.HCL)),
    SEQUENCE(Set.of(CodeLanguage.JAVA)),
    CLASS(Set.of(CodeLanguage.JAVA)),
    ENTITY_RELATIONSHIP(Set.of(CodeLanguage.JAVA)),
    INFRASTRUCTURE(Set.of(CodeLanguage.HCL));

    private final Set<CodeLanguage> supportedLanguages;

    DiagramType(Set<CodeLanguage> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public Set<CodeLanguage> getSupportedLanguages() {
        return supportedLanguages;
    }

    public boolean supportsLanguage(CodeLanguage language) {
        return supportedLanguages.contains(language);
    }
}
