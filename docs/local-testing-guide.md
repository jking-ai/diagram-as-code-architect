# Local Testing Guide

How to run tests, use the Bruno API test suite, and test manually with curl.

---

## 1. Automated Test Suite

Tests use mocked dependencies (no GCP credentials needed). JUnit 5 with Spring Boot Test.

```bash
cd backend

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests 'com.jkingai.diagramarchitect.service.CodeAnalysisServiceTest'

# Run tests matching a pattern
./gradlew test --tests '*DiagramController*'

# Verbose output
./gradlew test --info

# Force re-run (skip cache)
./gradlew test --rerun
```

### Test Classes (56 tests across 7 classes)

| Class | Package | What it covers |
|-------|---------|----------------|
| `DiagramArchitectApplicationTests` | root | Application context loads |
| `CodeAnalysisServiceTest` | service | Code validation, language/type compatibility, size limits |
| `PromptTemplateEngineTest` | service | Template selection, placeholder replacement, caching |
| `MermaidSyntaxExtractorTest` | service | Extraction from backtick-wrapped and raw LLM responses |
| `DiagramGenerationServiceTest` | service | Orchestration with mocked dependencies |
| `DiagramControllerTest` | controller | Endpoint behavior with mocked service, validation errors |
| `DiagramGenerationIntegrationTest` | controller | End-to-end with mocked ChatClient, full request/response cycle |

### Test Reports

After running tests, view the HTML report:

```bash
open backend/build/reports/tests/test/index.html
```

---

## 2. Bruno API Test Suite

A [Bruno](https://www.usebruno.com/) collection is included at `backend/bruno/` for interactive API testing against a running instance.

### Setup

1. Install Bruno 3.1+ from [usebruno.com](https://www.usebruno.com/)
2. Open Bruno and click **Open Collection**
3. Navigate to the `backend/bruno/` folder in this repo

### Environments

The collection includes a `local` environment pre-configured with:

- `host`: `http://localhost:8080`
- `apiKey`: `dev-local-key-changeme`

For production testing, copy `environments/production.bru.example` to `environments/production.bru` and fill in the real API key.

### Running the Tests

Start the backend first (see [Local Development Guide](local-dev-guide.md)):

```bash
cd backend
./gradlew bootRun
```

Then run requests in Bruno. The collection is organized into folders:

#### Health

| Request | Method | Auth | Description |
|---------|--------|------|-------------|
| Health Check | `GET /api/v1/health` | None | Verify the app is running |

#### Diagrams

| Request | Method | Auth | Description |
|---------|--------|------|-------------|
| Generate Flowchart (Java) | `POST /api/v1/diagrams/generate` | API Key | Generate a flowchart from Java code |
| Generate Infrastructure (HCL) | `POST /api/v1/diagrams/generate` | API Key | Generate infra diagram from Terraform |
| Get Types | `GET /api/v1/diagrams/types` | API Key | List all supported diagram types |
| Generate Missing Code | `POST /api/v1/diagrams/generate` | API Key | Expects 400 -- missing `code` field |
| Generate Unsupported | `POST /api/v1/diagrams/generate` | API Key | Expects 400 -- invalid type/language combo |
| Generate No API Key | `POST /api/v1/diagrams/generate` | None | Expects 401 -- missing API key |

---

## 3. Manual API Testing with curl

If you prefer curl over Bruno, here's the full workflow. Start the backend first.

### Health check (no auth)

```bash
curl -s http://localhost:8080/api/v1/health | python3 -m json.tool
```

### List diagram types

```bash
curl -s -H "X-API-Key: dev-local-key-changeme" \
  http://localhost:8080/api/v1/diagrams/types | python3 -m json.tool
```

### Generate a flowchart from Java

```bash
curl -s -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "@RestController\npublic class HelloController {\n    @GetMapping(\"/hello\")\n    public String hello() { return \"Hello\"; }\n}",
    "diagramType": "FLOWCHART",
    "codeLanguage": "JAVA"
  }' | python3 -m json.tool
```

### Generate an infrastructure diagram from Terraform

```bash
curl -s -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "resource \"google_compute_network\" \"vpc\" {\n  name = \"main-vpc\"\n  auto_create_subnetworks = false\n}\n\nresource \"google_container_cluster\" \"primary\" {\n  name     = \"app-cluster\"\n  location = \"us-central1\"\n  network  = google_compute_network.vpc.id\n}",
    "diagramType": "INFRASTRUCTURE",
    "codeLanguage": "HCL"
  }' | python3 -m json.tool
```

### Test error cases

```bash
# 400 -- missing required field
curl -s -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{"diagramType": "FLOWCHART", "codeLanguage": "JAVA"}' | python3 -m json.tool

# 400 -- unsupported combination
curl -s -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{"code": "resource \"aws_instance\" {}", "diagramType": "SEQUENCE", "codeLanguage": "HCL"}' | python3 -m json.tool

# 401 -- missing API key
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -d '{"code": "class Foo {}", "diagramType": "FLOWCHART", "codeLanguage": "JAVA"}'
```

---

## 4. Demo Script

A demo script is included that exercises all diagram types against a running backend:

```bash
# Run against production
./demo/demo.sh

# Run against local backend
./demo/demo.sh http://localhost:8080
```

The script:
1. Checks that the backend is running (health endpoint)
2. Generates a flowchart from Java sample code
3. Generates a sequence diagram from Java sample code
4. Generates a class diagram from Java sample code
5. Generates an infrastructure diagram from Terraform sample code
6. Prints each Mermaid output with a header

Requires `curl` and `jq`.

---

## 5. Troubleshooting

**Tests fail with compilation error**
Check Java version: `java --version` should be 21+.

**Bruno requests fail with connection refused**
The backend isn't running. Start it with `./gradlew bootRun`.

**Bruno requests return 401**
Select the `local` environment in Bruno (top-right dropdown). This sets the API key header automatically.

**curl returns 401**
Include the API key header: `-H "X-API-Key: dev-local-key-changeme"`. Health endpoint does not require auth.

**Demo script fails**
Ensure the backend is running and `jq` is installed (`brew install jq` on macOS).

**Generate returns LLM_ERROR**
The backend couldn't reach Vertex AI Gemini. Verify GCP credentials are configured (see [Local Development Guide](local-dev-guide.md#1-gcp-credentials-setup)).
