# API Contracts -- Diagram-as-Code Architect

Base URL: `/api/v1`

All request and response bodies use `application/json`.

---

## Authentication

All endpoints except **health** require the `X-API-Key` header.

| Header | Value | Required |
|--------|-------|----------|
| `X-API-Key` | A valid API key | Yes (except `/api/v1/health`) |

**How it works:**
- The backend validates the `X-API-Key` header via a Spring Security filter (`ApiKeyAuthenticationFilter`).
- **Local development:** Use the dev key `dev-local-key-changeme` (configured in `application-local.yml`).
- **Production:** The Firebase Function proxy (`apiProxy`) injects the API key server-side. Frontend calls are same-origin (`/api/**` → Cloud Function → Cloud Run), so the key is never exposed in client code.
- Requests to protected endpoints without a valid API key receive a `401 UNAUTHORIZED` response.

---

## Endpoints Overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/diagrams/generate` | `X-API-Key` | Generate a Mermaid.js diagram from source code |
| GET | `/api/v1/diagrams/types` | `X-API-Key` | List all supported diagram types |
| GET | `/api/v1/health` | None | Health check |

---

## Supported Diagram Types

```java
public enum DiagramType {
    FLOWCHART,            // Component/architecture overview
    SEQUENCE,             // Request flow through layers
    CLASS,                // Class hierarchy and relationships
    ENTITY_RELATIONSHIP,  // JPA entity relationships
    INFRASTRUCTURE        // Cloud infrastructure topology
}
```

```java
public enum CodeLanguage {
    JAVA,  // Spring Boot source code
    HCL    // Terraform configuration files
}
```

### Valid Diagram Type and Code Language Combinations

| Diagram Type | JAVA | HCL |
|---|---|---|
| `FLOWCHART` | Yes | Yes |
| `SEQUENCE` | Yes | No |
| `CLASS` | Yes | No |
| `ENTITY_RELATIONSHIP` | Yes | No |
| `INFRASTRUCTURE` | No | Yes |

Requesting an unsupported combination (e.g., `SEQUENCE` with `HCL`) returns a 400 error.

---

## 1. Generate Diagram

**`POST /api/v1/diagrams/generate`**

Accepts source code and a diagram type, analyzes the code using an LLM, and returns valid Mermaid.js diagram syntax.

### Request Body

```json
{
  "code": "string (required)",
  "diagramType": "string (required)",
  "codeLanguage": "string (required)",
  "context": "string (optional)"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | Yes | The source code to analyze. Can be a single file or multiple files concatenated with file path headers. Maximum 50,000 characters. |
| `diagramType` | string | Yes | One of: `FLOWCHART`, `SEQUENCE`, `CLASS`, `ENTITY_RELATIONSHIP`, `INFRASTRUCTURE` |
| `codeLanguage` | string | Yes | One of: `JAVA`, `HCL` |
| `context` | string | No | Optional natural-language context to guide diagram generation (e.g., "Focus on the authentication flow" or "Show only the VPC and compute resources"). Maximum 500 characters. |

### Example Request -- Spring Boot Flowchart

```bash
curl -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "@RestController\n@RequestMapping(\"/api/v1/orders\")\npublic class OrderController {\n\n    private final OrderService orderService;\n    private final PaymentService paymentService;\n    private final NotificationService notificationService;\n\n    public OrderController(OrderService orderService, PaymentService paymentService, NotificationService notificationService) {\n        this.orderService = orderService;\n        this.paymentService = paymentService;\n        this.notificationService = notificationService;\n    }\n\n    @PostMapping\n    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {\n        Order order = orderService.create(request);\n        paymentService.processPayment(order);\n        notificationService.sendConfirmation(order);\n        return ResponseEntity.status(HttpStatus.CREATED).body(order);\n    }\n\n    @GetMapping(\"/{id}\")\n    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {\n        return ResponseEntity.ok(orderService.findById(id));\n    }\n}\n\n@Service\npublic class OrderService {\n    private final OrderRepository orderRepository;\n    private final InventoryClient inventoryClient;\n\n    public Order create(CreateOrderRequest request) {\n        inventoryClient.reserveStock(request.getItems());\n        return orderRepository.save(new Order(request));\n    }\n\n    public Order findById(UUID id) {\n        return orderRepository.findById(id).orElseThrow();\n    }\n}\n\n@Service\npublic class PaymentService {\n    private final PaymentGatewayClient paymentGateway;\n\n    public void processPayment(Order order) {\n        paymentGateway.charge(order.getTotal(), order.getPaymentMethod());\n    }\n}\n\n@Service\npublic class NotificationService {\n    private final EmailClient emailClient;\n\n    public void sendConfirmation(Order order) {\n        emailClient.send(order.getCustomerEmail(), \"Order Confirmed\", order.getSummary());\n    }\n}",
    "diagramType": "FLOWCHART",
    "codeLanguage": "JAVA"
  }'
```

### Response -- 200 OK (Flowchart)

```json
{
  "mermaidSyntax": "flowchart TB\n    subgraph API[\"API Layer\"]\n        OC[OrderController]\n    end\n\n    subgraph Services[\"Service Layer\"]\n        OS[OrderService]\n        PS[PaymentService]\n        NS[NotificationService]\n    end\n\n    subgraph Persistence[\"Persistence Layer\"]\n        OR[OrderRepository]\n    end\n\n    subgraph External[\"External Clients\"]\n        IC[InventoryClient]\n        PG[PaymentGatewayClient]\n        EC[EmailClient]\n    end\n\n    OC --> OS\n    OC --> PS\n    OC --> NS\n    OS --> OR\n    OS --> IC\n    PS --> PG\n    NS --> EC",
  "diagramType": "FLOWCHART",
  "codeLanguage": "JAVA",
  "metadata": {
    "model": "gemini-3.1-flash-lite-preview",
    "inputCharacters": 1847,
    "processingTimeMs": 2130
  }
}
```

### Example Request -- Spring Boot Sequence Diagram

```bash
curl -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "@RestController\n@RequestMapping(\"/api/v1/orders\")\npublic class OrderController {\n\n    private final OrderService orderService;\n    private final PaymentService paymentService;\n    private final NotificationService notificationService;\n\n    @PostMapping\n    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {\n        Order order = orderService.create(request);\n        paymentService.processPayment(order);\n        notificationService.sendConfirmation(order);\n        return ResponseEntity.status(HttpStatus.CREATED).body(order);\n    }\n}\n\n@Service\npublic class OrderService {\n    private final OrderRepository orderRepository;\n    private final InventoryClient inventoryClient;\n\n    public Order create(CreateOrderRequest request) {\n        inventoryClient.reserveStock(request.getItems());\n        return orderRepository.save(new Order(request));\n    }\n}\n\n@Service\npublic class PaymentService {\n    private final PaymentGatewayClient paymentGateway;\n\n    public void processPayment(Order order) {\n        paymentGateway.charge(order.getTotal(), order.getPaymentMethod());\n    }\n}\n\n@Service\npublic class NotificationService {\n    private final EmailClient emailClient;\n\n    public void sendConfirmation(Order order) {\n        emailClient.send(order.getCustomerEmail(), \"Order Confirmed\", order.getSummary());\n    }\n}",
    "diagramType": "SEQUENCE",
    "codeLanguage": "JAVA",
    "context": "Focus on the createOrder flow"
  }'
```

### Response -- 200 OK (Sequence Diagram)

```json
{
  "mermaidSyntax": "sequenceDiagram\n    participant Client\n    participant OC as OrderController\n    participant OS as OrderService\n    participant IC as InventoryClient\n    participant OR as OrderRepository\n    participant PS as PaymentService\n    participant PG as PaymentGatewayClient\n    participant NS as NotificationService\n    participant EC as EmailClient\n\n    Client->>OC: POST /api/v1/orders\n    OC->>OS: create(request)\n    OS->>IC: reserveStock(items)\n    IC-->>OS: stock reserved\n    OS->>OR: save(order)\n    OR-->>OS: saved order\n    OS-->>OC: order\n    OC->>PS: processPayment(order)\n    PS->>PG: charge(total, paymentMethod)\n    PG-->>PS: payment confirmed\n    PS-->>OC: void\n    OC->>NS: sendConfirmation(order)\n    NS->>EC: send(email, subject, body)\n    EC-->>NS: sent\n    NS-->>OC: void\n    OC-->>Client: 201 Created (Order)",
  "diagramType": "SEQUENCE",
  "codeLanguage": "JAVA",
  "metadata": {
    "model": "gemini-3.1-flash-lite-preview",
    "inputCharacters": 1420,
    "processingTimeMs": 2450
  }
}
```

### Example Request -- Terraform Infrastructure Diagram

```bash
curl -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "resource \"google_compute_network\" \"vpc\" {\n  name                    = \"main-vpc\"\n  auto_create_subnetworks = false\n}\n\nresource \"google_compute_subnetwork\" \"private\" {\n  name          = \"private-subnet\"\n  ip_cidr_range = \"10.0.1.0/24\"\n  region        = \"us-central1\"\n  network       = google_compute_network.vpc.id\n}\n\nresource \"google_compute_subnetwork\" \"public\" {\n  name          = \"public-subnet\"\n  ip_cidr_range = \"10.0.2.0/24\"\n  region        = \"us-central1\"\n  network       = google_compute_network.vpc.id\n}\n\nresource \"google_container_cluster\" \"primary\" {\n  name     = \"app-cluster\"\n  location = \"us-central1\"\n  network  = google_compute_network.vpc.id\n  subnetwork = google_compute_subnetwork.private.id\n\n  initial_node_count = 3\n}\n\nresource \"google_sql_database_instance\" \"main\" {\n  name             = \"app-db\"\n  database_version = \"POSTGRES_16\"\n  region           = \"us-central1\"\n\n  settings {\n    tier = \"db-f1-micro\"\n    ip_configuration {\n      ipv4_enabled    = false\n      private_network = google_compute_network.vpc.id\n    }\n  }\n}\n\nresource \"google_compute_global_address\" \"lb\" {\n  name = \"app-lb-ip\"\n}\n\nresource \"google_compute_firewall\" \"allow_internal\" {\n  name    = \"allow-internal\"\n  network = google_compute_network.vpc.id\n\n  allow {\n    protocol = \"tcp\"\n    ports    = [\"0-65535\"]\n  }\n\n  source_ranges = [\"10.0.0.0/16\"]\n}",
    "diagramType": "INFRASTRUCTURE",
    "codeLanguage": "HCL"
  }'
```

### Response -- 200 OK (Infrastructure Diagram)

```json
{
  "mermaidSyntax": "flowchart TB\n    subgraph GCP[\"Google Cloud Platform - us-central1\"]\n        LB[\"Global Load Balancer\\napp-lb-ip\"]\n\n        subgraph VPC[\"main-vpc\"]\n            FW[\"Firewall: allow-internal\\ntcp 0-65535\\nsource: 10.0.0.0/16\"]\n\n            subgraph PubSubnet[\"public-subnet\\n10.0.2.0/24\"]\n            end\n\n            subgraph PrivSubnet[\"private-subnet\\n10.0.1.0/24\"]\n                GKE[\"GKE Cluster\\napp-cluster\\n3 nodes\"]\n                CSQL[\"Cloud SQL\\napp-db\\nPostgreSQL 16\\ndb-f1-micro\"]\n            end\n        end\n    end\n\n    LB --> PubSubnet\n    PubSubnet --> GKE\n    GKE --> CSQL\n    FW -.->|\"allows internal traffic\"| PrivSubnet",
  "diagramType": "INFRASTRUCTURE",
  "codeLanguage": "HCL",
  "metadata": {
    "model": "gemini-3.1-flash-lite-preview",
    "inputCharacters": 1135,
    "processingTimeMs": 1980
  }
}
```

### Example Request -- Java Class Diagram

```bash
curl -X POST http://localhost:8080/api/v1/diagrams/generate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-local-key-changeme" \
  -d '{
    "code": "public interface PaymentProcessor {\n    PaymentResult process(PaymentRequest request);\n}\n\n@Service\npublic class StripePaymentProcessor implements PaymentProcessor {\n    private final StripeClient stripeClient;\n    private final TransactionRepository transactionRepo;\n\n    @Override\n    public PaymentResult process(PaymentRequest request) {\n        ChargeResult charge = stripeClient.charge(request.getAmount());\n        Transaction tx = transactionRepo.save(new Transaction(charge));\n        return new PaymentResult(tx.getId(), charge.getStatus());\n    }\n}\n\npublic record PaymentRequest(BigDecimal amount, String currency, String paymentMethodId) {}\n\npublic record PaymentResult(UUID transactionId, String status) {}\n\n@Entity\npublic class Transaction {\n    @Id @GeneratedValue\n    private UUID id;\n    private BigDecimal amount;\n    private String status;\n    private Instant createdAt;\n}",
    "diagramType": "CLASS",
    "codeLanguage": "JAVA"
  }'
```

### Response -- 200 OK (Class Diagram)

```json
{
  "mermaidSyntax": "classDiagram\n    class PaymentProcessor {\n        <<interface>>\n        +process(PaymentRequest) PaymentResult\n    }\n\n    class StripePaymentProcessor {\n        -StripeClient stripeClient\n        -TransactionRepository transactionRepo\n        +process(PaymentRequest) PaymentResult\n    }\n\n    class PaymentRequest {\n        <<record>>\n        +BigDecimal amount\n        +String currency\n        +String paymentMethodId\n    }\n\n    class PaymentResult {\n        <<record>>\n        +UUID transactionId\n        +String status\n    }\n\n    class Transaction {\n        <<entity>>\n        -UUID id\n        -BigDecimal amount\n        -String status\n        -Instant createdAt\n    }\n\n    PaymentProcessor <|.. StripePaymentProcessor : implements\n    StripePaymentProcessor ..> PaymentRequest : uses\n    StripePaymentProcessor ..> PaymentResult : returns\n    StripePaymentProcessor --> TransactionRepository : uses\n    StripePaymentProcessor --> StripeClient : uses",
  "diagramType": "CLASS",
  "codeLanguage": "JAVA",
  "metadata": {
    "model": "gemini-3.1-flash-lite-preview",
    "inputCharacters": 892,
    "processingTimeMs": 1750
  }
}
```

### Response -- 401 Unauthorized (missing or invalid API key)

```json
{
  "error": "UNAUTHORIZED",
  "message": "Missing or invalid API key",
  "timestamp": "2026-02-22T14:29:00Z",
  "path": "/api/v1/diagrams/generate"
}
```

### Response -- 400 Bad Request (missing required field)

```json
{
  "error": "VALIDATION_ERROR",
  "message": "The 'code' field is required and must not be blank.",
  "timestamp": "2026-02-22T14:30:00Z",
  "path": "/api/v1/diagrams/generate"
}
```

### Response -- 400 Bad Request (unsupported combination)

```json
{
  "error": "UNSUPPORTED_DIAGRAM_TYPE",
  "message": "Diagram type SEQUENCE is not supported for code language HCL. Supported types for HCL: FLOWCHART, INFRASTRUCTURE.",
  "timestamp": "2026-02-22T14:31:00Z",
  "path": "/api/v1/diagrams/generate"
}
```

### Response -- 400 Bad Request (code too large)

```json
{
  "error": "CODE_TOO_LARGE",
  "message": "Code input exceeds the maximum allowed size of 50,000 characters. Received: 62,340 characters.",
  "timestamp": "2026-02-22T14:32:00Z",
  "path": "/api/v1/diagrams/generate"
}
```

### Response -- 502 Bad Gateway (LLM failure)

```json
{
  "error": "LLM_ERROR",
  "message": "The upstream AI service failed to generate a diagram. Please try again.",
  "timestamp": "2026-02-22T14:35:00Z",
  "path": "/api/v1/diagrams/generate"
}
```

---

## 2. List Supported Diagram Types

**`GET /api/v1/diagrams/types`**

Returns all supported diagram types with their compatible code languages and descriptions.

### Example Request

```bash
curl -H "X-API-Key: dev-local-key-changeme" http://localhost:8080/api/v1/diagrams/types
```

### Response -- 200 OK

```json
{
  "diagramTypes": [
    {
      "type": "FLOWCHART",
      "name": "Flowchart / Architecture Diagram",
      "description": "Component and architecture overview showing services, modules, and their connections.",
      "supportedLanguages": ["JAVA", "HCL"],
      "mermaidDirective": "flowchart"
    },
    {
      "type": "SEQUENCE",
      "name": "Sequence Diagram",
      "description": "Request flow through Spring Boot controllers, services, and repositories showing method call order.",
      "supportedLanguages": ["JAVA"],
      "mermaidDirective": "sequenceDiagram"
    },
    {
      "type": "CLASS",
      "name": "Class Diagram",
      "description": "Class hierarchy and relationships including interfaces, inheritance, and dependencies.",
      "supportedLanguages": ["JAVA"],
      "mermaidDirective": "classDiagram"
    },
    {
      "type": "ENTITY_RELATIONSHIP",
      "name": "Entity-Relationship Diagram",
      "description": "JPA entity relationships derived from annotations such as @OneToMany, @ManyToOne, and @ManyToMany.",
      "supportedLanguages": ["JAVA"],
      "mermaidDirective": "erDiagram"
    },
    {
      "type": "INFRASTRUCTURE",
      "name": "Infrastructure Topology Diagram",
      "description": "Cloud infrastructure topology derived from Terraform resource definitions, showing networks, compute, storage, and their connections.",
      "supportedLanguages": ["HCL"],
      "mermaidDirective": "flowchart"
    }
  ]
}
```

---

## 3. Health Check

**`GET /api/v1/health`**

Returns the health status of the application and its dependencies.

### Example Request

```bash
curl http://localhost:8080/api/v1/health
```

### Response -- 200 OK

```json
{
  "status": "UP",
  "components": {
    "chatModel": {
      "status": "UP",
      "details": {
        "model": "gemini-3.1-flash-lite-preview",
        "provider": "vertexai"
      }
    }
  }
}
```

### Response -- 503 Service Unavailable

```json
{
  "status": "DOWN",
  "components": {
    "chatModel": {
      "status": "DOWN",
      "details": {
        "error": "Unable to reach Vertex AI endpoint"
      }
    }
  }
}
```

---

## Error Response Format

All error responses follow a consistent structure:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description of what went wrong.",
  "timestamp": "2026-02-22T15:00:00Z",
  "path": "/api/v1/endpoint"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `error` | string | Machine-readable error code |
| `message` | string | Human-readable error description |
| `timestamp` | string (ISO 8601) | When the error occurred |
| `path` | string | The request path that triggered the error |

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Missing or invalid `X-API-Key` header |
| `VALIDATION_ERROR` | 400 | Request body failed validation (missing or blank required fields) |
| `UNSUPPORTED_DIAGRAM_TYPE` | 400 | The requested diagram type is not compatible with the given code language |
| `CODE_TOO_LARGE` | 400 | Code input exceeds the 50,000 character limit |
| `INVALID_CODE_LANGUAGE` | 400 | The code language value is not one of the supported enums |
| `INVALID_DIAGRAM_TYPE` | 400 | The diagram type value is not one of the supported enums |
| `GENERATION_FAILED` | 500 | Diagram generation failed during processing |
| `RATE_LIMIT_EXCEEDED` | 429 | Per-IP rate limit exceeded (Bucket4j, in-memory). Body contains `retryAfterSeconds`; response also includes `Retry-After` header. Defaults: 10/min burst general, 5/min burst on `/api/v1/diagrams/generate`. |
| `RATE_LIMITED` | 429 | Vertex AI rate limit exceeded (gRPC `RESOURCE_EXHAUSTED`). Response includes `Retry-After` header (seconds). |
| `LLM_ERROR` | 502 | The upstream Vertex AI Gemini service returned an error or timed out |
| `SERVICE_UNAVAILABLE` | 503 | Circuit breaker is open due to repeated LLM failures; requests are failing fast. Response includes `Retry-After` header (seconds). |

---

## Response DTO Schemas

### DiagramResponse

```json
{
  "mermaidSyntax": "string -- The generated Mermaid.js diagram syntax",
  "diagramType": "string -- The diagram type that was generated (echoed from request)",
  "codeLanguage": "string -- The code language that was analyzed (echoed from request)",
  "metadata": {
    "model": "string -- The LLM model used for generation",
    "inputCharacters": "integer -- Number of characters in the code input",
    "processingTimeMs": "integer -- Total processing time in milliseconds"
  }
}
```

### DiagramRequest

```json
{
  "code": "string -- Required. Source code to analyze. Max 50,000 characters.",
  "diagramType": "string -- Required. One of: FLOWCHART, SEQUENCE, CLASS, ENTITY_RELATIONSHIP, INFRASTRUCTURE",
  "codeLanguage": "string -- Required. One of: JAVA, HCL",
  "context": "string -- Optional. Guidance for diagram generation. Max 500 characters."
}
```
