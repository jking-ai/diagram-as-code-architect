package com.jkingai.diagramarchitect.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-3.1-flash-lite-preview}")
    private String modelName;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> chatModelDetails = Map.of(
                "status", "UP",
                "details", Map.of(
                        "model", modelName,
                        "provider", "vertexai"
                )
        );

        Map<String, Object> response = Map.of(
                "status", "UP",
                "components", Map.of(
                        "chatModel", chatModelDetails
                )
        );

        return ResponseEntity.ok(response);
    }
}
