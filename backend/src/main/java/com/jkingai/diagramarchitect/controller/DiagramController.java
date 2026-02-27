package com.jkingai.diagramarchitect.controller;

import com.jkingai.diagramarchitect.dto.DiagramRequest;
import com.jkingai.diagramarchitect.dto.DiagramResponse;
import com.jkingai.diagramarchitect.dto.DiagramTypeInfo;
import com.jkingai.diagramarchitect.model.CodeLanguage;
import com.jkingai.diagramarchitect.model.DiagramType;
import com.jkingai.diagramarchitect.service.DiagramGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagrams")
public class DiagramController {

    private static final Map<DiagramType, DiagramTypeInfo> TYPE_INFO = Map.of(
            DiagramType.FLOWCHART, new DiagramTypeInfo(
                    "FLOWCHART", "Flowchart / Architecture Diagram",
                    "Component and architecture overview showing services, modules, and their connections.",
                    List.of("JAVA", "HCL"), "flowchart"),
            DiagramType.SEQUENCE, new DiagramTypeInfo(
                    "SEQUENCE", "Sequence Diagram",
                    "Request flow through Spring Boot controllers, services, and repositories showing method call order.",
                    List.of("JAVA"), "sequenceDiagram"),
            DiagramType.CLASS, new DiagramTypeInfo(
                    "CLASS", "Class Diagram",
                    "Class hierarchy and relationships including interfaces, inheritance, and dependencies.",
                    List.of("JAVA"), "classDiagram"),
            DiagramType.ENTITY_RELATIONSHIP, new DiagramTypeInfo(
                    "ENTITY_RELATIONSHIP", "Entity-Relationship Diagram",
                    "JPA entity relationships derived from annotations such as @OneToMany, @ManyToOne, and @ManyToMany.",
                    List.of("JAVA"), "erDiagram"),
            DiagramType.INFRASTRUCTURE, new DiagramTypeInfo(
                    "INFRASTRUCTURE", "Infrastructure Topology Diagram",
                    "Cloud infrastructure topology derived from Terraform resource definitions, showing networks, compute, storage, and their connections.",
                    List.of("HCL"), "flowchart")
    );

    private final DiagramGenerationService generationService;

    public DiagramController(DiagramGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<DiagramResponse> generate(@Valid @RequestBody DiagramRequest request) {
        DiagramResponse response = generationService.generate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, List<DiagramTypeInfo>>> getTypes() {
        List<DiagramTypeInfo> types = Arrays.stream(DiagramType.values())
                .map(TYPE_INFO::get)
                .toList();
        return ResponseEntity.ok(Map.of("diagramTypes", types));
    }
}
