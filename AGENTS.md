# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

Diagram-as-Code Architect converts Spring Boot (Java) or Terraform (HCL) source code into Mermaid.js diagrams using Vertex AI Gemini 2.0 Flash via Spring AI. Stateless architecture: no database.

- **Backend:** Spring Boot 3.5.11 + Spring AI 1.1.2 (Java 21) on Cloud Run
- **Frontend:** Astro 5.17.1 single-page app with Mermaid.js 11.6.0 (CDN) on Firebase Hosting
- **Resilience:** Resilience4j 2.2.0 circuit breaker + Spring Retry with exponential backoff

## Build & Run Commands

### Backend (`backend/` directory)
```bash
cd backend
./gradlew bootRun              # Local dev server on :8080 (uses 'local' profile)
./gradlew test                 # Run all tests (48 tests, JUnit 5, no GCP creds needed)
./gradlew test --tests "com.jkingai.diagramarchitect.SomeTest"  # Single test class
./gradlew build                # Full build
./gradlew jibDockerBuild       # Build container image locally (no Dockerfile - uses Jib)
./gradlew jib                  # Build and push to Artifact Registry
```

### Frontend (`frontend/` directory)
```bash
cd frontend
npm install
npm run dev                    # Dev server on :4321
npm run build                  # Production build to ./dist/
npm run preview                # Preview production build
```

### Demo
```bash
./demo/demo.sh                 # Run against production (default)
./demo/demo.sh http://localhost:8080  # Run against local backend
```

### Deployment
```bash
firebase deploy --only hosting  # Deploy frontend (from project root)
```

## Architecture

### Request Flow
`DiagramController` -> `DiagramGenerationService` (orchestrator) -> `CodeAnalysisService` (validation) -> `PromptTemplateEngine` (template assembly) -> `ResilientLlmClient` (circuit breaker) -> Spring AI `ChatClient` (Gemini call) -> `MermaidSyntaxExtractor` (parse response)

### Key Design Decisions
- **Prompt templates** are plain text files at `backend/src/main/resources/prompt/templates/`, keyed by `{language}-{diagramType}.txt` (e.g., `java-flowchart.txt`). Cached in `ConcurrentHashMap` on first load.
- **Mermaid.js loaded from CDN** in the Astro page, not from npm. Script tags require `is:inline` attribute for Astro to preserve them.
- **Container images built with Jib** (Gradle plugin) -- no Dockerfile exists. Base image: `eclipse-temurin:21-jre`.
- **CORS origins** configured per Spring profile in `application-{profile}.yml`.
- **Frontend API base URL** is determined at runtime in `index.astro` based on `window.location.hostname`.
- **Retry + circuit breaker:** `ResilientLlmClient` wraps `ChatClient` with a Resilience4j `@CircuitBreaker`. A `RetryTemplate` in `AiConfig` handles transient gRPC errors with exponential backoff (3 attempts, 2s initial, 3x multiplier). `LlmRateLimitException` (429) and `LlmServiceUnavailableException` (503) are differentiated for frontend-specific error messages.

### Supported Diagram Types
| Diagram Type | Java | HCL |
|---|---|---|
| FLOWCHART | Yes | Yes |
| SEQUENCE | Yes | No |
| CLASS | Yes | No |
| ENTITY_RELATIONSHIP | Yes | No |
| INFRASTRUCTURE | No | Yes |

### API Endpoints
- `POST /api/v1/diagrams/generate` -- Generate diagram (accepts `code`, `diagramType`, `codeLanguage`, optional `context`)
- `GET /api/v1/diagrams/types` -- List supported diagram types
- `GET /api/v1/health` -- Health check

### Exception Handling
| Exception | HTTP Status | Error Code |
|---|---|---|
| `UnsupportedDiagramTypeException` | 400 | `UNSUPPORTED_DIAGRAM_TYPE` |
| `IllegalArgumentException` (code too large) | 400 | `CODE_TOO_LARGE` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `LlmRateLimitException` | 429 | `RATE_LIMITED` (with `Retry-After` header) |
| `DiagramGenerationException` (with cause) | 502 | `LLM_ERROR` |
| `LlmServiceUnavailableException` | 503 | `SERVICE_UNAVAILABLE` (with `Retry-After` header) |

### Spring Profiles
- `local` -- Debug logging, reads GCP credentials from `classpath:gcp-credentials.json`
- `prod` -- INFO logging, uses Cloud Run service account, CORS allows Firebase Hosting domains

## Environment Setup

Backend requires GCP credentials for Vertex AI Gemini. For local development, place a service account key at `backend/src/main/resources/gcp-credentials.json` (gitignored). In production, Cloud Run's service account (with "Vertex AI User" role) provides implicit auth.

## Project Documentation

Detailed specs live in `docs/`:
- [`docs/architecture.md`](docs/architecture.md) -- System architecture and design decisions
- [`docs/api-contracts.md`](docs/api-contracts.md) -- API endpoint specifications
- [`docs/milestones.md`](docs/milestones.md) -- Development phases and deliverables
