# DocMaestroEngine

A Spring Boot microservice that performs AI-powered inference and OCR extraction on PDF documents using **AWS Bedrock** and **AWS Textract**.

- Submit a base64-encoded PDF with one or more natural-language questions
- The service asynchronously invokes AWS Bedrock (Claude, Titan, etc.) to answer each question
- Optionally runs AWS Textract first to extract specific fields from the document and enrich the Bedrock response
- Results are persisted to a database and retrieved via a polling endpoint

**Author:** Ganesh Punde  
**Version:** 12.18.3  
**Java:** 21  
**Spring Boot:** 3.4.3

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Building](#building)
- [Running](#running)
- [API Reference](#api-reference)
- [Request & Response Examples](#request--response-examples)
- [Error Codes](#error-codes)
- [Project Structure](#project-structure)
- [Key Design Decisions](#key-design-decisions)

---

## Architecture Overview

```
Client
  │
  ▼
PdfInferenceController          ← REST entry point (/pdf/inference/*)
  │
  ▼
QueuedInferenceService          ← Enqueues request into in-memory queue
  │
  ▼  (scheduled poll)
InferenceServiceImpl            ← Core pipeline orchestrator
  │
  ├──► TextExtractionService    ← Uploads PDF to S3, runs Textract job,
  │                                maps extracted blocks to output fields
  │
  └──► BedrockRuntimeAsyncClient ← Sends question + PDF to AWS Bedrock
         │
         ▼
     InspectConverseResponse    ← Validates content type, triggers retry
         │
         ▼
     PostConverseFunction       ← Merges Bedrock + Textract JSON, persists answer
         │
         ▼
     InferenceResponseCompletionTask ← Waits for all queries, marks FAILED/SUCCESS
```

**Flow summary:**

1. Client POSTs a PDF + queries → receives a `request_id` immediately (HTTP 202)
2. Request is placed in `PdfInferenceRequestQueue`
3. A scheduled task dequeues and processes each request asynchronously
4. For each query, optionally Textract extracts structured fields from the PDF
5. AWS Bedrock answers the question using the PDF as context
6. Bedrock and Textract responses are merged and saved to the database
7. Client polls `GET /pdf/inference/outcome/{request_id}` for results

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 21+ |
| Gradle | 9.x |
| Oracle Database | 12c+ (or H2 for local testing) |
| AWS Account | With Bedrock, Textract, S3, STS access |
| AWS IAM Role | With permissions for Bedrock, Textract, S3 |

---

## Configuration

All configuration is in `src/main/resources/application.properties` (or `application-local.properties` for local development).

### Required Properties

```properties
# Database
spring.datasource.url=jdbc:oracle:thin:@<host>:<port>/<service>
spring.datasource.username=<username>
spring.datasource.password=<password>

# AWS Region and credentials
maestro.system.region=us-east-1
maestro.system.roleArn=arn:aws:iam::<account-id>:role/<role-name>
maestro.system.sessionName=docmaestro-session

# S3 bucket used as Textract input staging area
maestro.inference.config.idpS3Bucket=<your-s3-bucket>

# Comma-separated list of enabled Bedrock model IDs
maestro.gp.po.models=anthropic.claude-3-5-sonnet-20241022-v2:0

# Maximum PDF size in bytes (base64 decoded)
maestro.gp.po.size_limit=10485760

# Bedrock inference defaults
maestro.inference.config.temp=0.1
maestro.inference.config.topp=0.9
maestro.inference.config.maxWait=3600000
maestro.inference.config.retry=2
maestro.inference.config.retryDelay=2000
maestro.inference.config.retryMultiply=2

# Token-per-minute throttle limit
maestro.inference.config.tokenLimit=100000

# Textract job polling intervals (ms)
maestro.inference.config.textractSleepInterval=20000
maestro.inference.config.textractShortSleepInterval=5000
maestro.inference.config.textractCompletedJobStatus=SUCCEEDED,PARTIAL_SUCCESS

# Textract API rate limits (calls per second)
maestro.inference.config.textract.throttle.startDocTextLimit=5
maestro.inference.config.textract.throttle.getDocTextLimit=5

# Queue poll interval (ms)
maestro.system.poll.interval=30000
```

### Guardrail Configuration (optional)

```properties
maestro.inference.guardrail.enabled=true
maestro.inference.guardrail.id=<guardrail-id>
maestro.inference.guardrail.version=DRAFT
```

### Local Development (profile: `local`)

```properties
maestro.system.local=true
maestro.system.profile=default   # AWS CLI profile name
```

When `maestro.system.local=true`, the service uses `ProfileCredentialsProvider` instead of STS role assumption.

---

## Building

```bash
gradle clean build
```

To compile only (skip tests):

```bash
gradle compileJava
```

To run tests only:

```bash
gradle test
```

The bootable JAR is produced at `build/libs/DocMaestroEngine-<version>.jar`.

---

## Running

```bash
java -jar build/libs/DocMaestroEngine-<version>.jar \
  --spring.profiles.active=local
```

The service starts on port `8080` by default.

---

## API Reference

### POST `/pdf/inference/summarize`

Submit a PDF for AI inference. Returns immediately with a `request_id`.

**Request headers:**
```
Content-Type: application/json
Accept: application/json
```

**Request body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `pdf_data` | string | Yes | Base64-encoded PDF content |
| `model_id` | string | Yes | Bedrock model ID (must be in enabled list) |
| `queries` | array | Yes | One or more inference queries |
| `generic_info` | array | No | Key-value metadata (e.g. JSON schemas) |

**Query object fields:**

| Field | Type | Required | Description |
|---|---|---|---|
| `question` | string | Yes | Natural-language question to ask Bedrock |
| `question_key` | string | No | Identifier for this answer in the response |
| `mime_type` | string | No | Expected response format: `text/plain`, `application/json`, `application/xml` |
| `max_tokens` | integer | No | Max tokens for Bedrock response |
| `temperature` | decimal | No | Sampling temperature (0–1) |
| `top_p` | decimal | Yes | Top-p sampling value (0–1) |
| `extract_info` | object | No | Textract extraction configuration |
| `textract_only` | boolean | No | If true, skips Bedrock and returns Textract result only |

**Extraction info fields (`extract_info`):**

| Field | Type | Description |
|---|---|---|
| `extract_fields` | array | List of fields to extract from the document |

**Extraction field fields:**

| Field | Type | Description |
|---|---|---|
| `input_field_name` | string | Label text to locate in the document |
| `output_field_name` | string | Key name in the output JSON |
| `lower_page` | integer | Start page (inclusive) |
| `upper_page` | integer | End page (inclusive) |
| `bounding_box` | object | Spatial filter (header/footer/column thresholds) |
| `enum_traits` | object | Match extracted value against allowed answers or regex |
| `subsequent_field_label` | string | Stop extraction when this label is encountered |

**Response (HTTP 202):**

```json
{
  "message": "Accepted",
  "status": 202,
  "request_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### GET `/pdf/inference/outcome/{request_id}`

Poll for inference results.

**Response (HTTP 200):**

```json
{
  "status": "SUCCESS",
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "inferences": [
    {
      "query": "What is the patient's diagnosis?",
      "answer": "Type 2 Diabetes Mellitus",
      "question_key": "diagnosis",
      "mime_type": "text/plain"
    }
  ]
}
```

Possible `status` values: `IN_PROGRESS`, `SUCCESS`, `FAILED`, `UNKNOWN`

---

### GET `/pdf/inference/ocr/{request_id}`

Retrieve the raw Textract OCR output for a previously submitted request.

**Response (HTTP 200):**

```json
{
  "request_id": "550e8400-e29b-41d4-a716-446655440000",
  "rawJson": "{ ... full Textract block output ... }"
}
```

---

## Request & Response Examples

### Example 1 — Simple question answering

```json
POST /pdf/inference/summarize
{
  "pdf_data": "<base64-encoded-pdf>",
  "model_id": "anthropic.claude-3-5-sonnet-20241022-v2:0",
  "queries": [
    {
      "question": "Summarize the key findings in this document.",
      "question_key": "summary",
      "mime_type": "text/plain",
      "top_p": 0.9,
      "max_tokens": 1024,
      "temperature": 0.1
    }
  ]
}
```

### Example 2 — Structured JSON extraction with Textract

```json
POST /pdf/inference/summarize
{
  "pdf_data": "<base64-encoded-pdf>",
  "model_id": "anthropic.claude-3-5-sonnet-20241022-v2:0",
  "queries": [
    {
      "question": "Extract the patient information as JSON.",
      "question_key": "patient_info",
      "mime_type": "application/json",
      "top_p": 0.9,
      "max_tokens": 2048,
      "temperature": 0.0,
      "extract_info": {
        "extract_fields": [
          {
            "input_field_name": "Patient Name",
            "output_field_name": "patient/name",
            "lower_page": 1,
            "upper_page": 1
          },
          {
            "input_field_name": "Date of Birth",
            "output_field_name": "patient/dob",
            "lower_page": 1,
            "upper_page": 1
          },
          {
            "input_field_name": "Diagnosis",
            "output_field_name": "patient/diagnosis",
            "enum_traits": {
              "answers": ["Type 1 Diabetes", "Type 2 Diabetes", "Hypertension"]
            }
          }
        ]
      }
    }
  ]
}
```

### Example 3 — Textract only (no Bedrock)

```json
{
  "question": "Extract fields",
  "question_key": "extracted_fields",
  "mime_type": "application/json",
  "top_p": 0.9,
  "textract_only": true,
  "extract_info": {
    "extract_fields": [
      {
        "input_field_name": "Member ID",
        "output_field_name": "member_id"
      }
    ]
  }
}
```

---

## Error Codes

All error responses include an `error_code` field for programmatic handling.

| Code | Category | Description |
|---|---|---|
| `VAL-REQ-001` | Validation | Request or request ID is null or blank |
| `VAL-REQ-002` | Validation | requestId cannot be null or empty |
| `VAL-400` | Validation | Bean validation failure (field-level errors) |
| `VAL-PATH-001` | Validation | Missing path parameter |
| `INF-PIPE-001` | Inference | General inference pipeline error |
| `INF-BR-001` | Bedrock | Failed to invoke Bedrock Converse API |
| `INF-BR-002` | Bedrock | Bedrock retries exhausted |
| `INF-BR-003` | Bedrock | Bedrock response content type mismatch |
| `AWS-BR-GR-CONFIG-001` | Guardrail | Guardrail ID/version missing when guardrails enabled |
| `TXT-S3-001` | Textract | Failed to upload document to S3 |
| `TXT-JOB-001` | Textract | Textract returned no blocks |
| `TXT-JOB-002` | Textract | Textract job failed |
| `TXT-SER-001` | Textract | Failed to serialize Textract output |
| `CTX-001` | System | Spring ApplicationContext not initialized |
| `CFG-001` | Config | Required property missing or blank |
| `CFG-002` | Config | Property could not be parsed to required type |
| `DATA-001` | Data | No inference record found for request ID |
| `DATA-002` | Data | No OCR record found for request ID |
| `HTTP-400` | HTTP | Malformed or unreadable request body |
| `HTTP-405` | HTTP | HTTP method not supported |
| `HTTP-406` | HTTP | Media type not acceptable |
| `HTTP-415` | HTTP | Unsupported media type |
| `SYS-500` | System | Unexpected internal server error |

**Error response format:**

```json
{
  "message": "UUID : <trace-id> : requestId cannot be null or empty.",
  "status": 400,
  "request_id": null,
  "error_code": "VAL-REQ-002"
}
```

---

## Project Structure

```
src/main/java/com/maestro/po/ms/inference/
├── InferenceApplication.java           ← Spring Boot entry point
│
├── controller/
│   └── PdfInferenceController.java     ← REST endpoints
│
├── service/
│   ├── InferenceService.java           ← Service interface
│   ├── InferenceServiceImpl.java       ← Core pipeline (Bedrock + merge logic)
│   ├── QueuedInferenceService.java     ← Queue-based async variant
│   ├── PdfInferenceRequestQueue.java   ← In-memory ConcurrentLinkedQueue
│   ├── TextExtractionService.java      ← Textract orchestration
│   ├── InferenceRetryService.java      ← Spring Retry for Bedrock calls
│   ├── TokenThrottleService.java       ← TPM-based Bedrock throttle
│   ├── TextractThrottleService.java    ← Per-second Textract rate limiter
│   └── [Validator classes]             ← Bean validation implementations
│
├── provider/
│   ├── AwsClientConfiguration.java     ← AWS SDK bean factory
│   └── SpringContextHolder.java        ← Static ApplicationContext holder
│
├── handler/
│   └── CustomGlobalExceptionHandler.java ← @ControllerAdvice error handler
│
├── model/
│   ├── annotation/                     ← JPA entities (InferenceResponse, InferenceAnswer, etc.)
│   ├── rest/                           ← Request/response DTOs
│   └── interfaces/                     ← S3InferenceRequest interface
│
├── repository/                         ← Spring Data JPA repositories
├── config/                             ← AOP logging, MDC filter configs
├── filter/                             ← Request logging filters
├── annotations/                        ← Custom validation annotations
├── exception/                          ← Typed exceptions with error codes
├── constants/
│   └── ApplicationConstants.java       ← All error codes and messages
└── util/
    ├── ResponseFormatValidator.java    ← JSON/XML format validation
    └── TextractJsonSerializer.java     ← Textract Block → JSON serializer
```

---

## Key Design Decisions

**Async queue-based processing** — Requests are accepted immediately and queued. A scheduled task processes them, preventing request timeouts on large PDFs or slow Textract jobs.

**Textract + Bedrock merge** — When both Textract extraction and Bedrock inference are configured for the same query, the structured Textract output is deep-merged into the Bedrock JSON response. Textract values take precedence, ensuring high-accuracy field extraction is not overridden by model hallucination.

**Content type enforcement** — If Bedrock returns a response that does not match the requested `mime_type` (e.g. returns plain text when JSON was requested), the service automatically reprompts Bedrock with a correction instruction before falling back to retry.

**Token throttling** — `TokenThrottleService` monitors rolling one-minute token usage against a configurable limit, blocking new Bedrock calls when the limit is reached to avoid AWS throttling errors.

**Guardrail support** — AWS Bedrock Guardrails can be enabled per-deployment via configuration, with full trace logging for compliance auditing.

**Structured error codes** — Every exception carries a unique error code (e.g. `INF-BR-002`) returned in the API response, enabling clients to handle specific failure scenarios programmatically.
