# Diagram-as-Code Architect

**Automated documentation tool that converts Spring Boot source code or Terraform files into Mermaid.js diagrams using LLM-powered code analysis.**

**Live Demo:** https://diagram-architect.web.app/

---

## Problem Statement

Engineering teams frequently need architecture diagrams, sequence diagrams, and infrastructure topology maps, but these artifacts are tedious to create manually and become stale almost immediately as code changes. Developers end up with outdated Confluence pages and whiteboards that no longer reflect reality.

Diagram-as-Code Architect solves this by accepting source code (Spring Boot Java or Terraform HCL) as input, sending it to a Gemini LLM with structured prompts for code analysis, and producing valid Mermaid.js diagram syntax as output. A lightweight frontend renders the diagrams in the browser and allows users to preview, edit, and export them.

## Target User Persona

**Primary:** Software engineers and architects who want to quickly generate up-to-date architecture diagrams from their existing codebase without manually drawing them.

**Secondary:** AI/ML engineers evaluating the project as a demonstration of LLM-powered code-to-structured-logic translation and developer productivity tooling.

## Capabilities

| Diagram Type | Java | HCL | Description |
|---|---|---|---|
| **Flowchart** | Yes | Yes | Component/architecture overview showing services and their connections |
| **Sequence** | Yes | -- | Request flow through Spring Boot controllers, services, and repositories |
| **Class** | Yes | -- | Class hierarchy and relationships (extends, implements, uses) |
| **Entity-Relationship** | Yes | -- | JPA entity relationships derived from annotations |
| **Infrastructure** | -- | Yes | Cloud infrastructure topology from Terraform resources |

## Skills and Engineering Patterns Showcased

| Pattern | Description |
|---------|-------------|
| LLM-Powered Code Analysis | Using Gemini via Spring AI to parse and reason about code structure, extracting classes, dependencies, and infrastructure resources |
| Prompt Engineering | Designing structured prompts that guide the LLM to produce valid, well-organized Mermaid.js syntax from raw code |
| Spring AI Integration | Using Spring AI's ChatClient with Vertex AI Gemini for structured output generation |
| Resilience Engineering | Circuit breaker + retry with exponential backoff for LLM rate-limit handling |
| API Key Security | Spring Security filter + Firebase Function proxy for server-side API key injection |
| API Design | Clean RESTful API that accepts code input and returns structured diagram output |
| Frontend Rendering | Browser-based Mermaid.js rendering with live preview and export capabilities |
| Cloud-Native Deployment | Containerized Spring Boot backend on Cloud Run, static frontend on Firebase Hosting |

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend Runtime | Java | 21 (LTS) |
| Backend Framework | Spring Boot | 3.5.11 |
| AI Framework | Spring AI (Vertex AI Gemini) | 1.1.2 |
| Chat Model | Vertex AI Gemini 3.1 Flash-Lite | preview |
| Resilience | Resilience4j | 2.2.0 |
| Frontend | Astro + Mermaid.js (CDN) | 5.17.1 / 11.6.0 |
| API Proxy | Firebase Cloud Functions (Node.js) | v2 |
| Containerization | Jib (Gradle plugin) | 3.4.5 |
| Backend Hosting | Google Cloud Run | v2 |
| Frontend Hosting | Firebase Hosting | -- |

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](architecture.md) | System design, tech stack, design decisions, and source tree |
| [API Contracts](api-contracts.md) | Endpoint specs, request/response examples, authentication, error codes |
| [Milestones](milestones.md) | Development phases and deliverables (22/22 complete) |
| [Production Deployment](production-deployment.md) | GCP resource inventory, build & deploy commands, verification |
| [Local Development Guide](local-dev-guide.md) | Prerequisites, credential setup, running locally |
| [Local Testing Guide](local-testing-guide.md) | Automated tests, Bruno API collection, manual curl testing |

## Success Criteria

The project is considered **done** when all of the following are true:

1. **Code-to-Diagram Generation Works:** A user can submit Spring Boot source code or Terraform files via the API and receive valid Mermaid.js diagram syntax in response.
2. **Multiple Diagram Types Supported:** The system can generate at least three diagram types: flowcharts, sequence diagrams, and class diagrams for Java code, plus infrastructure topology diagrams for Terraform.
3. **Frontend Renders Diagrams:** A browser-based UI allows users to paste code, select a diagram type, and see the rendered Mermaid.js diagram with options to copy the Mermaid syntax or export as PNG/SVG.
4. **Deployed End-to-End:** The backend runs on Cloud Run and the frontend is hosted on Firebase Hosting, with API key authentication securing the backend.
5. **Health Check Passes:** A `/health` endpoint confirms the service and its dependencies (Gemini model) are operational.
6. **Demo-Ready:** A scripted demo can walk through submitting sample Spring Boot code, generating 2-3 diagram types, and rendering them in the browser.

## Level of Effort

**Low-Medium** -- 3 development phases, 22 deliverables. The core complexity is in prompt engineering to produce valid Mermaid.js syntax from code. Spring AI handles the LLM integration, and Mermaid.js handles rendering, keeping the application logic relatively thin.
