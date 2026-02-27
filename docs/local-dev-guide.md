# Local Development Guide

How to set up, run, and test Diagram-as-Code Architect on your local machine.

---

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| Java | 21+ | `java --version` |
| Node.js | 20+ | `node --version` |
| Gradle | 8.12+ | `./gradlew --version` (wrapper included) |
| Firebase CLI | 13+ | `firebase --version` |
| curl | any | `curl --version` |

> The Gradle wrapper (`./gradlew`) is included -- you do not need Gradle installed separately.

---

## 1. Environment Setup

```bash
# Copy the environment template and fill in your values
cp .env.example .env

# Set your GCP project ID and other values in .env
# GCP_PROJECT_ID=your-gcp-project-id

# Set up Firebase project
firebase use <your-project-id>
```

---

## 2. GCP Credentials Setup

The backend requires GCP credentials to call Vertex AI Gemini for diagram generation.

### Create a GCP Service Account

1. Go to the [GCP Console](https://console.cloud.google.com) and select your project
2. Navigate to **IAM & Admin > Service Accounts**
3. Click **Create Service Account**
4. Give it a name (e.g. `diagram-architect-local`)
5. Grant the role **Vertex AI User** (`roles/aiplatform.user`)
6. Click **Done**, then click on the new service account
7. Go to the **Keys** tab and click **Add Key > Create new key > JSON**
8. Save the downloaded JSON file as `backend/src/main/resources/gcp-credentials.json`

> This file is gitignored and will never be committed.

### Enable the Vertex AI API

If you haven't already:

```bash
gcloud services enable aiplatform.googleapis.com --project=your-gcp-project-id
```

---

## 3. Start the Backend

```bash
cd backend
./gradlew bootRun
```

The backend starts on `http://localhost:8080` using the `local` Spring profile, which:
- Reads GCP credentials from `classpath:gcp-credentials.json`
- Uses the dev API key `dev-local-key-changeme`
- Enables debug logging

### Verify it's running

```bash
curl -s http://localhost:8080/api/v1/health | python3 -m json.tool
```

Expected:

```json
{
  "status": "UP",
  "components": {
    "chatModel": {
      "status": "UP",
      "details": {
        "model": "gemini-2.0-flash",
        "provider": "vertexai"
      }
    }
  }
}
```

---

## 4. Start the Frontend

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:4321`. It detects `localhost` and sends API requests directly to `http://localhost:8080` with the dev API key header.

---

## 5. Quick Verification

### List diagram types

```bash
curl -s -H "X-API-Key: dev-local-key-changeme" \
  http://localhost:8080/api/v1/diagrams/types | python3 -m json.tool
```

### Generate a diagram

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

### Test authentication

```bash
# Should return 401
curl -s -w "\nHTTP Status: %{http_code}\n" \
  http://localhost:8080/api/v1/diagrams/types
```

---

## 6. API Key for Local Development

The local Spring profile uses a hardcoded dev key:

| Setting | Value |
|---------|-------|
| API Key | `dev-local-key-changeme` |
| Header | `X-API-Key: dev-local-key-changeme` |
| Config file | `backend/src/main/resources/application-local.yml` |

The frontend dev server automatically includes this header when running on `localhost`.

---

## 7. Run the Test Suite

Tests use mocked dependencies -- no GCP credentials needed.

```bash
cd backend
./gradlew test
```

This runs all 56 tests across 7 test classes. See the [Local Testing Guide](local-testing-guide.md) for details.

---

## 8. Run the Demo Script

The demo script calls all diagram endpoints against a running backend:

```bash
./demo/demo.sh                              # Run against local backend (default)
./demo/demo.sh https://your-cloud-run-url   # Run against production
```

Requires `curl` and `jq`.

---

## 9. Firebase Function (optional)

If you need to test the Firebase Function proxy locally:

```bash
cd functions
npm install
cp .env.example .env    # Edit API_TARGET if needed
```

The function is only needed for production-like testing. For normal local development, the frontend calls the backend directly.

---

## Spring Profiles

| Profile | Usage | API Key Source | GCP Credentials |
|---------|-------|----------------|-----------------|
| `local` | Local development | `dev-local-key-changeme` (hardcoded) | `classpath:gcp-credentials.json` |
| `prod` | Cloud Run production | `API_KEY` env var (Secret Manager) | Service account (implicit) |

---

## Troubleshooting

**Backend fails to start with credential error**
Verify `backend/src/main/resources/gcp-credentials.json` exists and contains a valid service account key. The service account needs the **Vertex AI User** role.

```bash
# Test that credentials work
gcloud auth activate-service-account --key-file=backend/src/main/resources/gcp-credentials.json
gcloud ai models list --project=your-gcp-project-id --region=us-central1
```

**Port 8080 already in use**
Another process is using port 8080. Find and stop it:

```bash
lsof -i :8080
```

**Frontend can't reach backend**
Make sure the backend is running on `:8080`. The frontend dev server on `:4321` sends requests to `http://localhost:8080` when it detects `localhost`.

**Tests fail**
Tests are self-contained and don't require GCP credentials. If they fail, check Java version (`java --version` should be 21+).

**401 on API calls**
Include the API key header: `-H "X-API-Key: dev-local-key-changeme"`. The health endpoint (`/api/v1/health`) does not require auth.
