# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

Diagram-as-Code Architect converts Spring Boot (Java) or Terraform (HCL) source code into Mermaid.js diagrams using Vertex AI Gemini 3.1 Flash-Lite via Spring AI. Stateless architecture: no database.

- **Backend:** Spring Boot 3.5.11 + Spring AI 1.1.2 (Java 21) on Cloud Run
- **Frontend:** Astro 5.17.1 single-page app with Mermaid.js 11.6.0 (CDN) on Firebase Hosting
- **Proxy:** Firebase Cloud Function (`apiProxy`) injects API key and forwards to Cloud Run
- **Resilience:** Resilience4j 2.2.0 circuit breaker + Spring Retry with exponential backoff

## Build & Run Commands

### Backend (`backend/` directory)
```bash
cd backend
./gradlew bootRun              # Local dev server on :8080 (uses 'local' profile)
./gradlew test                 # Run all tests (52 tests, JUnit 5, no GCP creds needed)
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
./demo/demo.sh                        # Run against local backend (default)
./demo/demo.sh https://your-cloud-run-url  # Run against production
```

### Deployment
```bash
firebase deploy                        # Deploy hosting + functions
firebase deploy --only hosting         # Deploy frontend only
firebase deploy --only functions       # Deploy proxy function only
```

### Bruno API Tests (`backend/bruno/`)
Open the `backend/bruno/` collection in Bruno. Select the `local` environment for local development. Copy `production.bru.example` to `production.bru` and fill in the real API key for production testing.

## Architecture

### Request Flow (Production)
`Browser` -> `Firebase Hosting` (`/api/**` rewrite) -> `Cloud Function (apiProxy)` (injects `X-API-Key`) -> `Cloud Run` -> `DiagramController` -> `DiagramGenerationService` -> `CodeAnalysisService` -> `PromptTemplateEngine` -> `ResilientLlmClient` -> Spring AI `ChatClient` -> `MermaidSyntaxExtractor`

### Request Flow (Local Dev)
`Browser (:4321)` -> `Backend (:8080)` (with `X-API-Key: dev-local-key-changeme` header)

### Key Design Decisions
- **API Key authentication:** Spring Security filter (`ApiKeyAuthenticationFilter`) validates `X-API-Key` header. The key is injected server-side by the Firebase Function proxy -- never exposed in frontend code.
- **Prompt templates** are plain text files at `backend/src/main/resources/prompt/templates/`, keyed by `{language}-{diagramType}.txt` (e.g., `java-flowchart.txt`). Cached in `ConcurrentHashMap` on first load.
- **Mermaid.js loaded from CDN** in the Astro page, not from npm. Script tags require `is:inline` attribute for Astro to preserve them.
- **Container images built with Jib** (Gradle plugin) -- no Dockerfile exists. Base image: `eclipse-temurin:21-jre`.
- **CORS + Security** configured together in `SecurityConfig.java`. Allowed origins set per Spring profile in `application-{profile}.yml`.
- **Frontend API base URL** is determined at runtime in `index.astro` based on `window.location.hostname`. Local dev sends API key header directly; production uses same-origin calls via the Firebase Function proxy.
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
All endpoints except health and actuator require the `X-API-Key` header.

- `POST /api/v1/diagrams/generate` -- Generate diagram (accepts `code`, `diagramType`, `codeLanguage`, optional `context`). **Requires `X-API-Key`.**
- `GET /api/v1/diagrams/types` -- List supported diagram types. **Requires `X-API-Key`.**
- `GET /api/v1/health` -- Health check (no auth required)

### Exception Handling
| Exception | HTTP Status | Error Code |
|---|---|---|
| Missing/invalid API key | 401 | `UNAUTHORIZED` |
| `UnsupportedDiagramTypeException` | 400 | `UNSUPPORTED_DIAGRAM_TYPE` |
| `IllegalArgumentException` (code too large) | 400 | `CODE_TOO_LARGE` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `LlmRateLimitException` | 429 | `RATE_LIMITED` (with `Retry-After` header) |
| `DiagramGenerationException` (with cause) | 502 | `LLM_ERROR` |
| `LlmServiceUnavailableException` | 503 | `SERVICE_UNAVAILABLE` (with `Retry-After` header) |

### Spring Profiles
- `local` -- Debug logging, reads GCP credentials from `classpath:gcp-credentials.json`, API key: `dev-local-key-changeme`
- `prod` -- INFO logging, uses Cloud Run service account, API key from `API_KEY` env var (GCP Secret Manager)

## Environment Setup

Copy `.env.example` to `.env` and fill in your values (`GCP_PROJECT_ID`, `CLOUD_RUN_URL`, `API_KEY`). Run `firebase use <your-project-id>` to configure Firebase locally (creates `.firebaserc`, which is gitignored).

Backend requires GCP credentials for Vertex AI Gemini. For local development, place a service account key at `backend/src/main/resources/gcp-credentials.json` (gitignored). In production, Cloud Run's service account (with "Vertex AI User" role) provides implicit auth.

### API Key Setup
- **Local dev:** Uses `dev-local-key-changeme` (hardcoded in `application-local.yml` and frontend dev code)
- **Production:** Create a GCP Secret Manager secret named `DIAGRAM_ARCHITECT_API_KEY`. Set as `API_KEY` env var on Cloud Run. The Firebase Function reads it via `defineSecret('DIAGRAM_ARCHITECT_API_KEY')`.

### Firebase Function Setup
```bash
cd functions
npm install
cp .env.example .env           # Edit API_TARGET if needed
```
The function's `DIAGRAM_ARCHITECT_API_KEY` secret must be created in GCP Secret Manager before deploying.

## Project Documentation

Detailed specs live in `docs/`:
- [`docs/README.md`](docs/README.md) -- Project overview, capabilities, and quick links
- [`docs/architecture.md`](docs/architecture.md) -- System architecture and design decisions
- [`docs/api-contracts.md`](docs/api-contracts.md) -- API endpoint specifications
- [`docs/milestones.md`](docs/milestones.md) -- Development phases and deliverables (22/22 complete)
- [`docs/production-deployment.md`](docs/production-deployment.md) -- GCP deployment guide
- [`docs/local-dev-guide.md`](docs/local-dev-guide.md) -- Local development setup
- [`docs/local-testing-guide.md`](docs/local-testing-guide.md) -- Automated tests, Bruno collection, manual curl testing
