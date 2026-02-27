# Diagram-as-Code Architect

**Automated documentation tool that converts Spring Boot source code or Terraform files into Mermaid.js diagrams using LLM-powered code analysis.**

**Live Demo:** https://diagram-architect.web.app/

**Project Status:** All 3 phases complete (20/20 deliverables)

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.5.11 + Spring AI 1.1.2 (Java 21) |
| LLM | Vertex AI Gemini 2.0 Flash |
| Resilience | Resilience4j 2.2.0 (circuit breaker + retry) |
| Frontend | Astro 5.17.1 + Mermaid.js 11.6.0 (CDN) |
| Deployment | Cloud Run (backend) + Firebase Hosting (frontend) |
| Containerization | Jib (no Dockerfile) |

---

## Problem Statement

Engineering teams frequently need architecture diagrams, sequence diagrams, and infrastructure topology maps, but these artifacts are tedious to create manually and become stale almost immediately as code changes. Developers end up with outdated Confluence pages and whiteboards that no longer reflect reality.

Diagram-as-Code Architect solves this by accepting source code (Spring Boot Java or Terraform HCL) as input, sending it to a Gemini LLM with structured prompts for code analysis, and producing valid Mermaid.js diagram syntax as output. A lightweight frontend renders the diagrams in the browser and allows users to preview, edit, and export them.

## Target User Persona

**Primary:** Software engineers and architects who want to quickly generate up-to-date architecture diagrams from their existing codebase without manually drawing them.

**Secondary:** AI/ML engineers evaluating the project as a demonstration of LLM-powered code-to-structured-logic translation and developer productivity tooling.

## Skills and Engineering Patterns Showcased

| Pattern | Description |
|---------|-------------|
| LLM-Powered Code Analysis | Using Gemini via Spring AI to parse and reason about code structure, extracting classes, dependencies, and infrastructure resources |
| Prompt Engineering | Designing structured prompts that guide the LLM to produce valid, well-organized Mermaid.js syntax from raw code |
| Spring AI Integration | Using Spring AI's ChatClient with Vertex AI Gemini for structured output generation |
| Resilience Engineering | Circuit breaker + retry with exponential backoff for LLM rate-limit handling |
| API Design | Clean RESTful API that accepts code input and returns structured diagram output |
| Frontend Rendering | Browser-based Mermaid.js rendering with live preview and export capabilities |
| Cloud-Native Deployment | Containerized Spring Boot backend on Cloud Run, static frontend on Firebase Hosting |

---

## Quick Start

### Backend
```bash
cd backend
./gradlew bootRun    # Starts on http://localhost:8080 (requires GCP credentials)
./gradlew test       # Run all 48 tests (no GCP credentials needed)
```

### Frontend
```bash
cd frontend
npm install
npm run dev          # Starts on http://localhost:4321
```

### Demo
```bash
./demo/demo.sh                        # Run against production
./demo/demo.sh http://localhost:8080   # Run against local backend
```

---

## Project Structure

```
diagram-as-code-architect/
|-- backend/          # Spring Boot API (Java 21)
|-- frontend/         # Astro single-page app
|-- demo/             # Demo script and sample data
|-- docs/             # Architecture, API contracts, milestones
|-- firebase.json     # Firebase Hosting config
```

---

## Documentation

- [Architecture](docs/architecture.md) -- System design, tech stack, and design decisions
- [API Contracts](docs/api-contracts.md) -- Endpoint specs, request/response examples, error codes
- [Milestones](docs/milestones.md) -- Development phases and deliverables

---

## Success Criteria

The project is considered **done** when all of the following are true:

1. **Code-to-Diagram Generation Works:** A user can submit Spring Boot source code or Terraform files via the API and receive valid Mermaid.js diagram syntax in response.
2. **Multiple Diagram Types Supported:** The system can generate at least three diagram types: flowcharts (architecture/component diagrams), sequence diagrams, and class diagrams for Java code, plus infrastructure topology diagrams for Terraform.
3. **Frontend Renders Diagrams:** A browser-based UI allows users to paste code, select a diagram type, and see the rendered Mermaid.js diagram with options to copy the Mermaid syntax or export as PNG/SVG.
4. **Deployed End-to-End:** The backend runs on Cloud Run and the frontend is hosted on Firebase Hosting.
5. **Health Check Passes:** A `/health` endpoint confirms the service and its dependencies (Gemini model) are operational.
6. **Demo-Ready:** A scripted demo can walk through submitting sample Spring Boot code, generating 2-3 diagram types, and rendering them in the browser.

## Level of Effort

**Low-Medium** -- Estimated 3 development phases. The core complexity is in prompt engineering to produce valid Mermaid.js syntax from code. Spring AI handles the LLM integration, and Mermaid.js handles rendering, keeping the application logic relatively thin.
