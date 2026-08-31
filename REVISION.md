# DOT Field — Comprehensive Technical Architecture & Interview Guide

This document is the exhaustive technical revision guide for the **DOT Field Job Intelligence Platform**. It provides a deep-dive analysis of system architecture, security models, database migrations, job pipelines, requirement extraction algorithms, fit score calculations, resume tailoring engines, and deployment specifications.

---

## Table of Contents
1. [Platform Overview & Core Mission](#1-platform-overview--core-mission)
2. [Full Technology Stack](#2-full-technology-stack)
3. [System Architecture & Layer Separation](#3-system-architecture--layer-separation)
4. [Database Schema & Flyway Migration Hierarchy](#4-database-schema--flyway-migration-hierarchy)
5. [Authentication, JWT Architecture & Stateless Security](#5-authentication-jwt-architecture--stateless-security)
6. [Role-Based Access Control (RBAC)](#6-role-based-access-control-rbac)
7. [Candidate Privacy, Cross-User Data Isolation & IDOR Protection](#7-candidate-privacy-cross-user-data-isolation--idor-protection)
8. [Job Discovery, Ingestion & Extraction Engine](#8-job-discovery-ingestion--extraction-engine)
9. [Job Normalization & 3-Level Deduplication Hierarchy](#9-job-normalization--3-level-deduplication-hierarchy)
10. [Requirement Extraction & Fit Match Scoring Engine](#10-requirement-extraction--fit-match-scoring-engine)
11. [Authentic Resume Tailoring Engine](#11-authentic-resume-tailoring-engine)
12. [REST API Architecture & Standardized Error Handling](#12-rest-api-architecture--standardized-error-handling)
13. [Production Configuration & Secrets Management](#13-production-configuration--secrets-management)
14. [Testing Strategy & E2E Validation Matrix](#14-testing-strategy--e2e-validation-matrix)
15. [Production Deployment Readiness Guide](#15-production-deployment-readiness-guide)

---

## 1. Platform Overview & Core Mission

**DOT Field** is an AI-powered job discovery, requirement analysis, candidate match scoring, data protection, and authentic resume tailoring platform.

> [!IMPORTANT]
> **Manual Application Disclaimer**: DOT Field does NOT auto-submit job applications. Candidates manually apply through original job listings after analyzing fit scores and generating tailored resumes.

### Core Capabilities
* **Job Discovery & Aggregation**: Ingests job listings from corporate career pages and job boards.
* **Extraction & Normalization**: Standardizes salary formats, employment types (Full-time, Part-time, Contract, Internship), remote status (Remote, Hybrid, On-site), and canonical URLs.
* **3-Level Deduplication**: Merges repeated listings based on Source + External ID, Canonical URL, or SHA-256 Composite Fingerprint without uncontrolled data duplication.
* **Requirement Extraction**: Parses job descriptions to extract required vs. preferred skills, minimum experience years, degree levels, and location constraints.
* **Fit Scoring Engine**: Evaluates candidate profiles against job requirements across four dimensions (Skills 40%, Experience 30%, Education 15%, Location 15%), placing fit into categories: `EXCELLENT_MATCH`, `GOOD_MATCH`, `MODERATE_MATCH`, `WEAK_MATCH`.
* **Authentic Resume Tailoring**: Generates tailored resumes prioritizing matching experiences and projects strictly from candidate-provided data — **zero AI hallucination or data fabrication**.

---

## 2. Full Technology Stack

| Layer | Technology | Version / Tooling | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend** | React | 19.x | Declarative UI components, state, hooks (`useCallback`, `useState`, `useEffect`) |
| | Vite | 8.x | ES Module bundler, hot module replacement (HMR), production client build |
| | React Router DOM | 7.x | Single Page Application (SPA) routing (`/`, `/dashboard`, `/dashboard/:id`, `/login`, `/register`) |
| | Vanilla CSS3 | Custom CSS | Glassmorphism styling, responsive layouts, micro-animations |
| **Backend** | Java | 21 LTS | Core runtime with modern language features (Pattern matching, Records) |
| | Spring Boot | 3.4.1 | Application framework (`web`, `data-jpa`, `security`, `validation`, `flyway`) |
| | Spring Security | 6.x | Stateless JWT authentication, role authorization, security headers |
| | JJWT | 0.12.6 | HMAC-SHA256 JWT signing, claims verification, token parsing |
| **Database** | PostgreSQL | 14+ | Production RDBMS with relational integrity, constraints, and indexes |
| | Flyway | 10.x | Version-controlled database schema migrations (`V1`, `V2`) |
| | Hibernate / JPA | 6.x | ORM mapping with production `spring.jpa.hibernate.ddl-auto=validate` |
| | H2 Database | In-memory | Isolated unit testing runtime |

---

## 3. System Architecture & Layer Separation

DOT Field enforces strict clean architectural separation:

```
[ Frontend: React SPA ]
         │ (HTTP REST / JSON Envelope)
         ▼
[ Security Layer: Spring Security Filter Chain ]
         │ (JwtAuthenticationFilter -> SecurityContextHolder)
         ▼
[ Controller Layer: @RestController ]
         │ (Maps DTOs, validates @Valid payloads, delegates to Service)
         ▼
[ Service Layer: @Service @Transactional ]
         │ (Business logic, ownership isolation, match scoring, tailoring)
         ▼
[ Repository Layer: Spring Data JPA ]
         │ (Entities: User, Profile, Skill, Experience, Education, Project, Job)
         ▼
[ Database: PostgreSQL / Flyway Schema ]
```

---

## 4. Database Schema & Flyway Migration Hierarchy

Flyway owns all database schema structural creation. Production Hibernate runs in `validate` mode.

```
src/main/resources/db/migration/
├── V1__baseline_dot_field_schema.sql
└── V2__add_authentication.sql
```

### Table Definitions & Foreign Keys
1. `users`: Stores user identity (`id`, `email`, `password_hash`, `role`, `created_at`, `updated_at`).
2. `profiles`: Candidate profile owned by user (`id`, `user_id` [FK → `users.id`], `name`, `email`, `phone`, `location`, `linkedin_url`, `github_url`, `portfolio_url`). Unique constraint on `user_id`.
3. `skills`: Candidate skills (`id`, `profile_id` [FK → `profiles.id`], `name`, `category`).
4. `experiences`: Work history (`id`, `profile_id` [FK → `profiles.id`], `company`, `role`, `description`, `start_date`, `end_date`).
5. `educations`: Academic background (`id`, `profile_id` [FK → `profiles.id`], `institution`, `degree`, `field_of_study`, `start_date`, `end_date`, `grade`).
6. `projects`: Portfolio projects (`id`, `profile_id` [FK → `profiles.id`], `name`, `description`, `github_url`, `live_url`).
7. `project_technologies`: Element collection (`project_id` [FK → `projects.id`], `technology`).
8. `jobs`: Global job repository (`id`, `external_id`, `title`, `company`, `location`, `description`, `job_url`, `canonical_url`, `deduplication_fingerprint`, `source`, `employment_type`, `remote_type`, `status`, `salary_min`, `salary_max`, `currency`, `posted_date`, `last_discovered_at`).
   - Unique Index: `uk_jobs_source_external_id` (`source`, `external_id`)
   - Unique Index: `uk_jobs_canonical_url` (`canonical_url`)
   - Index: `idx_jobs_fingerprint` (`deduplication_fingerprint`)

---

## 5. Authentication, JWT Architecture & Stateless Security

* **Stateless Session Management**: `SessionCreationPolicy.STATELESS` avoids server session state.
* **Password Security**: Passwords are hashed using `BCryptPasswordEncoder`. Plaintext passwords are never persisted or logged.
* **JWT Identity Token**:
  - Algorithm: HMAC-SHA256 (requires minimum 32-byte / 256-bit secret)
  - Claims: `sub` = `userId`, `email`, `role`
  - Token transport: `Authorization: Bearer <token>`
* **CurrentUserService**: Central helper extracting authenticated `userId` and fetching current user's profile directly from `SecurityContextHolder`.

---

## 6. Role-Based Access Control (RBAC)

Two distinct roles are enforced via Spring Security and `@EnableMethodSecurity`:

| Role | Permitted Operations |
| :--- | :--- |
| `USER` | Read own profile & background data, update profile, browse jobs (`GET /jobs`, `GET /jobs/{id}`), calculate match score (`GET /jobs/{id}/match`), tailor resume (`GET /jobs/{id}/resume/tailor`). |
| `ADMIN` | All `USER` permissions PLUS administrative job creation (`POST /jobs`), discovery (`POST /jobs/discover`), extraction (`POST /jobs/extract`), updates (`PUT /jobs/{id}`), status patching (`PATCH /jobs/{id}/status`), and deletion (`DELETE /jobs/{id}`). |

Unauthenticated requests to protected routes return HTTP `401 Unauthorized`. Unauthorized operations return HTTP `403 Forbidden`.

---

## 7. Candidate Privacy, Cross-User Data Isolation & IDOR Protection

Candidate data isolation is strictly enforced at the backend service/repository layer:

1. **User Ownership**: Every profile entity (`Skill`, `Experience`, `Education`, `Project`) references `profile_id`, which maps to `user_id`.
2. **Repository Scoping**: Lookups fetch entities filtered by current authenticated user's profile ID (`findByProfileIdAndId`).
3. **IDOR Defense**: If User B attempts to access or modify User A's resource via ID manipulation (`/profile/experience/999`), the service layer receives empty `Optional` and throws `ResourceNotFoundException`, returning HTTP `404 Not Found` without exposing existence or contents of foreign data.

---

## 8. Job Discovery, Ingestion & Extraction Engine

1. **Sources**: `CompanyCareerPageSource` and custom adapters query external job boards/pages.
2. **Extraction Pipeline**: `JobExtractionPipeline` parses raw content into structured `ExtractedJob` objects.
3. **Isolation in Ingestion**: Each discovered listing persists in a dedicated `REQUIRES_NEW` transaction via `JobDiscoveryPersistenceHelper`. Constraint violations on one listing do not roll back the discovery batch.

---

## 9. Job Normalization & 3-Level Deduplication Hierarchy

The system applies a 3-tier deduplication check before creating job records:

```
Ingested Job Listing
       │
       ├── Level 1: (source + externalId) Match? ──[Yes]──> Update Existing Job
       │                  │ [No]
       ├── Level 2: canonicalUrl Match? ────────────[Yes]──> Update Existing Job
       │                  │ [No]
       └── Level 3: SHA-256 Composite Fingerprint? ─[Yes]──> Update Existing Job
                          │ [No]
                          ▼
                  Save New Job Record
```

* **Canonicalization**: Strips URL tracking parameters (`utm_source`, `utm_medium`, `ref`, `fbclid`, etc.), lowercases scheme/host, and normalizes paths.
* **SHA-256 Composite Fingerprint**: Computed as `SHA-256(normalize(company) + "|" + normalize(title) + "|" + normalize(location) [+ "|" + normalize(description)])`. If company, title, or location is missing, fingerprint returns `null` to avoid false duplicate merges.

---

## 10. Requirement Extraction & Fit Match Scoring Engine

### Score Breakdown (Total 100 Points)
* **Skills Match (40 Points)**: Evaluates required skills (high weight) and preferred skills (medium weight) using normalized skill dictionaries (`SkillNormalizationUtil`).
* **Experience Match (30 Points)**: Compares required experience years against total candidate work history.
* **Education Match (15 Points)**: Compares required degree level (`BACHELORS`, `MASTERS`, `DOCTORATE`) against candidate degrees using `DegreeLevel` hierarchy.
* **Location Match (15 Points)**: Compares job location and remote status against candidate location.

### Match Categories
* `EXCELLENT_MATCH`: Score ≥ 80
* `GOOD_MATCH`: 65 ≤ Score < 80
* `MODERATE_MATCH`: 50 ≤ Score < 65
* `WEAK_MATCH`: Score < 50

---

## 11. Authentic Resume Tailoring Engine

The `ResumeTailoringEngine` generates customized resume payloads without AI hallucinations:

1. **Section Selection**: Selects skills, experiences, projects, and education strictly present in the candidate profile.
2. **Keyword Match Matrix**: Highlights matching keywords between candidate experience descriptions and job requirements.
3. **Emphasis Rules**: Prioritizes work experiences and projects that contain matching domain keywords (`emphasized = true`).
4. **Zero Fabrication**: If candidate profile lacks a skill or section, it is marked missing or omitted — never invented.

---

## 12. REST API Architecture & Standardized Error Handling

All REST APIs return responses wrapped in standard JSON envelopes:

### Success Response Envelope
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully"
}
```

### Standard Error Response Envelope (`ApiError`)
```json
{
  "status": 404,
  "message": "Resource not found with id: 999",
  "timestamp": "2026-08-31T17:15:00",
  "errors": null
}
```

### HTTP Status Code Mapping
* `400 Bad Request`: Validation failure (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`).
* `401 Unauthorized`: Unauthenticated request or invalid JWT.
* `403 Forbidden`: Authenticated user lacks required role (`ADMIN`).
* `404 Not Found`: Resource or foreign IDOR target not found.
* `405 Method Not Allowed`: Unsupported HTTP method (`HttpRequestMethodNotSupportedException`).
* `409 Conflict`: Database unique constraint violation.
* `500 Internal Server Error`: Unexpected server error (stack traces hidden from client).

---

## 13. Production Configuration & Secrets Management

Production settings require environment variable configuration without fallbacks:

| Property | Environment Variable | Rule / Requirement |
| :--- | :--- | :--- |
| `spring.datasource.url` | `DB_URL` | PostgreSQL JDBC Connection String |
| `spring.datasource.username` | `DB_USERNAME` | Production Database User |
| `spring.datasource.password` | `DB_PASSWORD` | Production Database Password (No hardcoded fallback) |
| `jwt.secret` | `JWT_SECRET` | HMAC-SHA256 Secret (Minimum 32 bytes / 256 bits) |
| `jwt.expiration` | `JWT_EXPIRATION` | Token Expiration in ms (Default: 86400000 = 24h) |
| `cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (Wildcard `*` prohibited) |

---

## 14. Testing Strategy & E2E Validation Matrix

The platform is verified by comprehensive unit, integration, security, and E2E tests:

```text
Area                         Test Class / Verification Method              Status Target
──────────────────────────────────────────────────────────────────────────────────────────
Backend test suite           mvnw clean test                               PASS (250+ tests)
Frontend build               npm run build                                 PASS
Frontend lint                npm run lint                                  PASS
Fresh PostgreSQL             FreshDatabaseSchemaValidationTest             PASS
Flyway migrations            Flyway V1 -> V2 execution                     PASS
Hibernate validation         spring.jpa.hibernate.ddl-auto=validate        PASS
Registration                 AuthenticationSecurityTest                    PASS
Login                        AuthenticationSecurityTest                    PASS
JWT validation               AuthenticationSecurityTest                    PASS
RBAC                         RbacSecurityTest                              PASS
Cross-user isolation         IdorAndCrossUserSecurityTest                  PASS
IDOR protection              IdorAndCrossUserSecurityTest                  PASS
Job E2E pipeline             FullJobIntelligencePipelineE2ETest            PASS
Deduplication                JobPipelineReliabilityTest                    PASS
Malformed job handling       JobPipelineReliabilityTest                    PASS
API error handling           StandardizedApiErrorTest                      PASS
CORS                         CorsAndSecurityHeadersTest                    PASS
Production secrets           ProductionConfigurationFailureTest            PASS
Health check                 HealthController (/api/health)                PASS
Production smoke test        ProductionSmokeIntegrationTest                PASS
Documentation                REVISION.md & README.md                       PASS
```

---

## 15. Production Deployment Readiness Guide

### Pre-Deployment Checklist
1. Export environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `INITIAL_ADMIN_EMAIL`).
2. Verify PostgreSQL database is accessible and empty or migrated.
3. Build production backend jar:
   ```bash
   cd dot-field-backend
   .\mvnw.cmd clean package -DskipTests=false
   ```
4. Build production frontend assets:
   ```bash
   npm run build
   ```
5. Launch application and verify health:
   ```bash
   java -jar target/dot-field-backend-0.0.1-SNAPSHOT.jar
   # Probe endpoint: GET http://localhost:8080/api/health
   # Response: {"success": true, "data": {"status": "UP", "database": "UP"}}
   ```
