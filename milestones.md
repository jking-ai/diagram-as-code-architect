# Milestones -- Diagram-as-Code Architect

This document defines three development phases with concrete deliverables and acceptance criteria. Each phase builds on the previous one. An AI agent or developer should be able to execute against these milestones using the companion documents (architecture.md and api-contracts.md) as references.

---

## Phase 1: Foundation ✅

**Goal:** Project scaffolding, core dependencies wired, prompt templates drafted, and a deployable (but non-functional) backend and frontend skeleton running locally.

**Estimated Effort:** 1-2 days

### Deliverables

#### 1.1 Backend Project Scaffolding ✅

- [x] Initialize a Spring Boot 3.5.11 project using Gradle (Kotlin DSL) with Java 21.
- [x] Configure the following dependencies in `build.gradle.kts`:
  - `spring-boot-starter-web`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator`
  - `spring-ai-starter-model-vertex-ai-gemini` (Spring AI 1.1.2)
  - Test dependencies: `spring-boot-starter-test`
- [x] Create the package structure as defined in architecture.md under `backend/src/main/java/com/jkingai/diagramarchitect/`.
- [x] Configure `application.yml` with profiles for `local` and `prod`.
- [x] In `application.yml`, configure Spring AI Vertex AI properties:
  - `spring.ai.vertex.ai.gemini.project-id` (from environment variable)
  - `spring.ai.vertex.ai.gemini.location` (default: `us-central1`)
  - `spring.ai.vertex.ai.gemini.chat.options.model` (default: `gemini-2.0-flash`)
  - `spring.ai.vertex.ai.gemini.chat.options.temperature` (default: `0.2` for deterministic structured output)

**Acceptance Criteria:**
- [x] `./gradlew build` completes successfully (compilation, no test failures).
- [x] The application starts locally with `./gradlew bootRun` using the `local` profile.

#### 1.2 Model Enums and DTOs ✅

- [x] Create `DiagramType` enum with values: `FLOWCHART`, `SEQUENCE`, `CLASS`, `ENTITY_RELATIONSHIP`, `INFRASTRUCTURE`.
- [x] Create `CodeLanguage` enum with values: `JAVA`, `HCL`.
- [x] Add a method to `DiagramType` that returns the set of supported `CodeLanguage` values for that type (matching the compatibility table in api-contracts.md).
- [x] Create `DiagramRequest` record with fields: `code`, `diagramType`, `codeLanguage`, `context`. Add Bean Validation annotations: `@NotBlank` on `code`, `@NotNull` on `diagramType` and `codeLanguage`, `@Size(max = 50000)` on `code`, `@Size(max = 500)` on `context`.
- [x] Create `DiagramResponse` record with fields: `mermaidSyntax`, `diagramType`, `codeLanguage`, `metadata`.
- [x] Create `DiagramTypeInfo` record with fields: `type`, `name`, `description`, `supportedLanguages`, `mermaidDirective`.
- [x] Create `ErrorResponse` record with fields: `error`, `message`, `timestamp`, `path`.

**Acceptance Criteria:**
- [x] All DTOs compile and have proper validation annotations.
- [x] `DiagramType.SEQUENCE.getSupportedLanguages()` returns `[JAVA]`.
- [x] `DiagramType.FLOWCHART.getSupportedLanguages()` returns `[JAVA, HCL]`.
- [x] `DiagramType.INFRASTRUCTURE.getSupportedLanguages()` returns `[HCL]`.

#### 1.3 Health Check Endpoint ✅

- [x] Implement `GET /api/v1/health` as specified in api-contracts.md.
- [x] The endpoint checks Vertex AI availability by making a lightweight test call (or returning `UNKNOWN` if the model is not yet configured).
- [x] Return the response structure matching api-contracts.md.

**Acceptance Criteria:**
- [x] `curl http://localhost:8080/api/v1/health` returns a 200 response with the expected JSON structure.

#### 1.4 CORS Configuration ✅

- [x] Create `CorsConfig.java` that allows requests from `http://localhost:5000` (Firebase local emulator) and the production Firebase Hosting domain.
- [x] Configure allowed methods: `GET`, `POST`, `OPTIONS`.
- [x] Configure allowed headers: `Content-Type`.

**Acceptance Criteria:**
- [x] A preflight `OPTIONS` request from `http://localhost:5000` returns appropriate CORS headers.

#### 1.5 Frontend Skeleton ✅

- [x] Create Astro 5.17.1 single-page app (`frontend/src/pages/index.astro`) with: a code input textarea, a diagram type dropdown selector, a code language dropdown selector, an optional context input field, a "Generate" button, a diagram output area, and buttons for "Copy Mermaid", "Export PNG", and "Export SVG".
- [x] Dark theme with CSS variables matching the portfolio site aesthetic (implemented inline in Astro component).
- [x] Event listeners wired to all UI elements with generation logic.
- [x] API integration calling backend endpoints with dynamic base URL (localhost vs production).
- [x] Mermaid.js initialized from CDN (`https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js`) with `is:inline` attribute for Astro compatibility.
- [x] `firebase.json` with hosting configuration pointing to `frontend/dist/` as the public folder.

**Acceptance Criteria:**
- [x] Opening the frontend in a browser shows the full UI layout.
- [x] The diagram type dropdown is populated with all five types.
- [x] Mermaid.js loads from CDN without errors.
- [x] Diagrams render correctly in the output area.

---

**Phase 1 Dependencies:** None. This is the starting phase.

---

## Phase 2: Core Features ✅

**Goal:** End-to-end diagram generation working -- user submits code, backend processes it through the LLM, and the frontend renders the resulting Mermaid diagram.

**Estimated Effort:** 2-4 days

**Depends on:** Phase 1 complete (scaffolding, DTOs, health check, frontend skeleton).

### Deliverables

#### 2.1 Prompt Templates ✅

- [x] Create prompt template files in `backend/src/main/resources/prompt/templates/`:
  - `java-flowchart.txt`
  - `java-sequence.txt`
  - `java-class.txt`
  - `java-entity-relationship.txt`
  - `hcl-flowchart.txt`
  - `hcl-infrastructure.txt`
- [x] Each template includes:
  - A system instruction explaining the task.
  - Explicit Mermaid syntax rules for the target diagram type.
  - A one-shot example of valid input and valid Mermaid output.
  - Placeholders `{code}` and `{context}`.
  - An instruction to output ONLY the Mermaid code block, with no surrounding explanation.
- [x] Templates validated by testing against the Gemini model.

**Acceptance Criteria:**
- [x] Six prompt template files exist and contain all required sections.
- [x] Each template, when filled with sample code, produces a valid prompt string.

#### 2.2 Prompt Template Engine ✅

- [x] Implement `PromptTemplateEngine.java` that:
  - Loads prompt templates from the classpath.
  - Selects the correct template based on `CodeLanguage` and `DiagramType`.
  - Replaces `{code}` and `{context}` placeholders with actual values.
  - Returns the assembled prompt string.
  - Caches templates in `ConcurrentHashMap` on first load.
- [x] If `context` is null or blank, replace `{context}` with an empty string or a default instruction.

**Acceptance Criteria:**
- [x] Given `JAVA` and `FLOWCHART`, the engine returns a prompt containing the java-flowchart template content with placeholders filled.
- [x] Given `HCL` and `SEQUENCE`, the engine throws `UnsupportedDiagramTypeException`.

#### 2.3 Code Analysis Service ✅

- [x] Implement `CodeAnalysisService.java` that:
  - Validates code is not blank and does not exceed 50,000 characters.
  - Validates the `DiagramType` and `CodeLanguage` combination is supported.
  - Trims and normalizes whitespace in the code input.
- [x] Throws `UnsupportedDiagramTypeException` for invalid combinations.

**Acceptance Criteria:**
- [x] Blank code input throws a validation exception.
- [x] Code exceeding 50,000 characters throws a validation exception.
- [x] `SEQUENCE` with `HCL` throws `UnsupportedDiagramTypeException`.
- [x] `FLOWCHART` with `JAVA` passes validation.

#### 2.4 Mermaid Syntax Extractor ✅

- [x] Implement `MermaidSyntaxExtractor.java` that:
  - Parses the LLM response text to extract the Mermaid code block.
  - Handles responses wrapped in triple backticks (` ```mermaid ... ``` `) or returned as raw Mermaid syntax.
  - Strips any leading/trailing whitespace or markdown formatting.
  - Performs basic validation: ensures the extracted syntax starts with a valid Mermaid directive (`flowchart`, `sequenceDiagram`, `classDiagram`, `erDiagram`).
- [x] Throws `DiagramGenerationException` if no valid Mermaid syntax can be extracted.

**Acceptance Criteria:**
- [x] Extracts Mermaid from ` ```mermaid\nflowchart TB\n...\n``` `.
- [x] Extracts Mermaid from raw `flowchart TB\n...` without backticks.
- [x] Throws exception for a response with no valid Mermaid content.

#### 2.5 Diagram Generation Service ✅

- [x] Implement `DiagramGenerationService.java` that orchestrates the full flow:
  1. Calls `CodeAnalysisService` to validate the input.
  2. Calls `PromptTemplateEngine` to assemble the prompt.
  3. Sends the prompt to Vertex AI Gemini via `ResilientLlmClient` (wraps Spring AI `ChatClient` with Resilience4j circuit breaker).
  4. Calls `MermaidSyntaxExtractor` to parse the response.
  5. Returns a `DiagramResponse` with the Mermaid syntax and metadata (model name, input character count, processing time).
- [x] Configure the `ChatClient` bean in `AiConfig.java` using Spring AI auto-configuration for Vertex AI Gemini.
- [x] Set the temperature to `0.2` for consistent, deterministic output.
- [x] Custom `RetryTemplate` bean with exponential backoff (3 attempts, 2s initial, 3x multiplier) that retries on gRPC `RESOURCE_EXHAUSTED` errors.
- [x] Differentiates rate-limit errors (`LlmRateLimitException`) from generic LLM errors.

**Acceptance Criteria:**
- [x] Submitting code with `FLOWCHART`/`JAVA` returns a `DiagramResponse` containing valid Mermaid syntax starting with `flowchart`.
- [x] The `metadata.processingTimeMs` field reflects actual elapsed time.
- [x] The `metadata.model` field is `gemini-2.0-flash`.

#### 2.6 Diagram Controller ✅

- [x] Implement `DiagramController.java` with:
  - `POST /api/v1/diagrams/generate` -- accepts `DiagramRequest`, calls `DiagramGenerationService`, returns `DiagramResponse`.
  - `GET /api/v1/diagrams/types` -- returns the list of all supported diagram types as specified in api-contracts.md.
- [x] Add `@Valid` annotation on the `@RequestBody DiagramRequest` parameter.

**Acceptance Criteria:**
- [x] `POST /api/v1/diagrams/generate` with valid input returns 200 with a `DiagramResponse`.
- [x] `POST /api/v1/diagrams/generate` with missing `code` returns 400 `VALIDATION_ERROR`.
- [x] `GET /api/v1/diagrams/types` returns all five diagram types with correct metadata.

#### 2.7 Global Exception Handling ✅

- [x] Implement `GlobalExceptionHandler` using `@RestControllerAdvice`.
- [x] Map `UnsupportedDiagramTypeException` to 400 with `UNSUPPORTED_DIAGRAM_TYPE`.
- [x] Map `DiagramGenerationException` to 502 with `LLM_ERROR` (when caused by upstream failure) or 500 with `GENERATION_FAILED`.
- [x] Map `MethodArgumentNotValidException` to 400 with `VALIDATION_ERROR`.
- [x] Map `LlmRateLimitException` to 429 with `RATE_LIMITED` and `Retry-After` header.
- [x] Map `LlmServiceUnavailableException` to 503 with `SERVICE_UNAVAILABLE` and `Retry-After` header.
- [x] Map generic exceptions to 500 with a safe message (no stack traces in the response).
- [x] All error responses follow the standard format from api-contracts.md.

**Acceptance Criteria:**
- [x] Invalid requests return properly formatted error JSON matching the error response schema.
- [x] LLM failures return 502 with `LLM_ERROR`.
- [x] Rate-limited requests return 429 with `RATE_LIMITED`.
- [x] Exceptions during generation return 500 with a safe message; full details are logged server-side.

#### 2.8 Frontend Integration ✅

- [x] API calls wired in Astro inline script (replaces separate api.js/app.js/renderer.js):
  - `fetch(POST /api/v1/diagrams/generate)` on Generate button click.
  - Dynamic base URL based on `window.location.hostname`.
- [x] On "Generate" button click: collect form values, call API, pass `mermaidSyntax` to `mermaid.render()`.
- [x] Loading spinner shown while request is in flight.
- [x] Error banner displays API error messages, with specific messages for 429 and 503 responses.
- [x] Diagram type dropdown filtered based on selected code language (HCL hides SEQUENCE, CLASS, ENTITY_RELATIONSHIP).
- [x] `mermaid.render()` inserts SVG into output area; render failures display raw syntax with error message.

**Acceptance Criteria:**
- [x] User can paste Java code, select `FLOWCHART`, click "Generate", and see a rendered diagram.
- [x] User can paste Terraform code, select `INFRASTRUCTURE`, click "Generate", and see a rendered diagram.
- [x] Selecting `HCL` as the code language filters out Java-only diagram types from the dropdown.
- [x] API errors display a visible error message in the UI.

---

## Phase 3: Polish and Demo (In Progress)

**Goal:** Export functionality, production deployment, and a working demo.

**Estimated Effort:** 1-2 days

**Depends on:** Phase 2 complete (end-to-end diagram generation and rendering working).

### Deliverables

#### 3.1 Export Functionality ✅

- [x] Implemented inline in the Astro component (`frontend/src/pages/index.astro`):
  - Copy Mermaid syntax to clipboard via `navigator.clipboard`.
  - Export as PNG via Canvas 2D API with 2x scaling for quality.
  - Export as SVG by extracting SVG markup and triggering download.
- [x] "Copy Mermaid", "Export PNG", and "Export SVG" buttons wired and functional.

**Acceptance Criteria:**
- [x] Clicking "Copy Mermaid" copies the syntax to the clipboard (verified by pasting).
- [x] Clicking "Export PNG" downloads a PNG file of the diagram.
- [x] Clicking "Export SVG" downloads an SVG file of the diagram.

#### 3.2 Container Image with Jib ✅

- [x] Jib Gradle plugin (v3.4.5) added to `build.gradle.kts`.
- [x] Base image: `eclipse-temurin:21-jre`.
- [x] Image target: `us-central1-docker.pkg.dev/jking-ai-labs/docker-repo/diagram-architect-api`.
- [x] Container config: JVM flags (`-Xms256m -Xmx512m`), port 8080, `SPRING_PROFILES_ACTIVE=prod`.

**Acceptance Criteria:**
- [x] `./gradlew jibDockerBuild` produces a local Docker image.
- [x] The image starts and the health endpoint responds.

#### 3.3 Cloud Run Deployment ✅

- [x] Backend deployed to Cloud Run via `./gradlew jib` + `gcloud run deploy`.
- [x] Environment configured for `prod` profile with GCP project ID and Vertex AI region.
- [x] Cloud Run service account has the `Vertex AI User` IAM role.

**Acceptance Criteria:**
- [x] The backend deploys to Cloud Run and starts successfully.
- [x] `curl https://diagram-architect-api-153583612125.us-central1.run.app/api/v1/health` returns `status: UP`.
- [x] `curl https://diagram-architect-api-153583612125.us-central1.run.app/api/v1/diagrams/types` returns the diagram types list.

#### 3.4 Firebase Hosting Deployment ✅

- [x] `firebase.json` configured with:
  - Public directory: `frontend/dist/`.
  - Rewrite rule: all paths serve `index.html` (SPA support).
  - Cache headers for static assets (`*.js`, `*.css`): `public, max-age=31536000, immutable`.
- [x] API base URL determined at runtime based on `window.location.hostname`.
- [x] Deployed with `firebase deploy --only hosting`.

**Acceptance Criteria:**
- [x] The frontend is accessible at https://diagram-architect.web.app/.
- [x] The frontend successfully calls the Cloud Run backend and renders diagrams.

#### 3.5 Unit and Integration Tests

- [ ] Write unit tests for all services:
  - `CodeAnalysisServiceTest` -- validation logic.
  - `PromptTemplateEngineTest` -- template selection and placeholder replacement.
  - `MermaidSyntaxExtractorTest` -- extraction from various LLM response formats.
  - `DiagramGenerationServiceTest` -- orchestration with mocked dependencies.
- [ ] Write controller tests:
  - `DiagramControllerTest` -- endpoint behavior with mocked `DiagramGenerationService`.
- [ ] Write one integration test:
  - `DiagramGenerationIntegrationTest` -- end-to-end with a mocked ChatClient that returns a canned Mermaid response. Verifies the full request/response cycle through the controller.
- [ ] Add sample test resources:
  - `sample-spring-boot-code.java` -- sample Java input.
  - `sample-terraform.tf` -- sample Terraform input.
  - `expected-flowchart.mmd` -- expected Mermaid output for validation.

**Acceptance Criteria:**
- `./gradlew test` passes with all tests green.
- Tests do not require external GCP credentials (the ChatClient is mocked).

#### 3.6 Demo Script and Sample Data

- [ ] Create a `demo/` directory in the project root with:
  - `sample-order-service.java` -- a multi-class Spring Boot code sample (the OrderController example from api-contracts.md).
  - `sample-infrastructure.tf` -- a Terraform configuration sample (the VPC/GKE example from api-contracts.md).
  - `demo.sh` -- a shell script that:
    1. Checks that the backend is running (calls health endpoint).
    2. Generates a flowchart from the Java sample.
    3. Generates a sequence diagram from the Java sample.
    4. Generates a class diagram from the Java sample.
    5. Generates an infrastructure diagram from the Terraform sample.
    6. Prints each Mermaid output with a header.
- [ ] The demo script should use `curl` and `jq` for readable output.

**Acceptance Criteria:**
- Running `./demo/demo.sh` against a running backend instance completes without errors.
- Each request returns valid Mermaid syntax.
- The demo takes under 1 minute to run end-to-end.

---

## Milestone Dependency Graph

```mermaid
flowchart LR
    P1["Phase 1: Foundation"]
    P2["Phase 2: Core Features"]
    P3["Phase 3: Polish and Demo"]

    P1 --> P2 --> P3

    subgraph Phase1["Phase 1 Deliverables"]
        direction TB
        S1[1.1 Backend Scaffolding]
        S2[1.2 Enums and DTOs]
        S3[1.3 Health Check]
        S4[1.4 CORS Config]
        S5[1.5 Frontend Skeleton]
        S1 --> S2 --> S3
        S1 --> S4
        S5
    end

    subgraph Phase2["Phase 2 Deliverables"]
        direction TB
        C1[2.1 Prompt Templates]
        C2[2.2 Prompt Template Engine]
        C3[2.3 Code Analysis Service]
        C4[2.4 Mermaid Syntax Extractor]
        C5[2.5 Diagram Generation Service]
        C6[2.6 Diagram Controller]
        C7[2.7 Exception Handling]
        C8[2.8 Frontend Integration]
        C1 --> C2
        C2 --> C5
        C3 --> C5
        C4 --> C5
        C5 --> C6
        C7
        C6 --> C8
    end

    subgraph Phase3["Phase 3 Deliverables"]
        direction TB
        D1[3.1 Export Functionality]
        D2[3.2 Container Image]
        D3[3.3 Cloud Run Deploy]
        D4[3.4 Firebase Hosting Deploy]
        D5[3.5 Tests]
        D6[3.6 Demo Script]
        D2 --> D3
        D3 --> D4
        D1
        D5
        D6
    end
```

---

## Summary Table

| Phase | Deliverables | Depends On | Status |
|-------|-------------|------------|--------|
| Phase 1: Foundation | Backend scaffolding, enums/DTOs, health check, CORS config, frontend skeleton | None | ✅ Complete |
| Phase 2: Core Features | Prompt templates, template engine, code analysis, Mermaid extractor, generation service, controller, error handling, frontend integration | Phase 1 | ✅ Complete |
| Phase 3: Polish and Demo | Export functionality, container image, Cloud Run deploy, Firebase Hosting deploy, tests, demo script | Phase 2 | 4/6 complete (tests + demo remaining) |
| **Total** | **18/20 deliverables complete** | | |
