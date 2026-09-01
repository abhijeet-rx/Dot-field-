# DOT Field — Job Intelligence Platform

> DOT Field is an AI-powered job discovery, requirement analysis, fit matching, candidate data protection, and resume tailoring platform.

> [!IMPORTANT]
> **Application Disclaimer**: DOT Field does not automatically apply to jobs. Users manually apply through the original job listing.

---

## Overview

DOT Field helps candidates discover relevant job opportunities, analyze job requirements against their authentic candidate profile, score job fit, generate tailored resumes, and manage job opportunities — while preserving candidate privacy and manual application workflows.

---

## Tech Stack

- **Frontend**: React 19, Vite, React Router DOM, CSS3 (Glassmorphism & Micro-animations)
- **Backend**: Java 21, Spring Boot 3.4.1 (Web, Data JPA, Security, Validation, Flyway)
- **Security**: Spring Security 6, JJWT (0.12.6), BCrypt Password Hashing, Stateless Sessions
- **Database**: PostgreSQL (Production/Dev), H2 (In-Memory Unit Testing)

---

## Environment Variables & Configuration

The application requires environment variables in production and development. Copy `.env.example` to `.env` in `dot-field-backend/` or set variables in your deployment environment:

| Variable | Description | Default / Required |
|----------|-------------|-------------------|
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/dot_field` |
| `DB_USERNAME` | PostgreSQL database user | `postgres` |
| `DB_PASSWORD` | PostgreSQL database password | *(Required — no hardcoded fallback)* |
| `JWT_SECRET` | Secret key for HMAC-SHA256 JWT signing | *(Required for HS256 — min 32 chars)* |
| `JWT_EXPIRATION` | Token expiration time in milliseconds | `86400000` (24h) |
| `JWT_ALGORITHM` | JWT verification algorithm (`HS256` or `RS256`) | `HS256` |
| `JWT_JWKS_URL` | Remote JWKS URL for RS256 token verification | *(Optional — 15m cache TTL)* |
| `JWT_PUBLIC_KEY_PATH` | RSA public key string or path for RS256 | *(Optional)* |
| `JOB_INGESTION_SCHEDULER_ENABLED` | Enables scheduled background job ingestion | `false` *(Disabled by default)* |
| `JOB_CANONICALIZE_SCHEME` | Normalizes `http` and `https` scheme equivalence | `true` |
| `RATE_LIMIT_DISCOVERY_CAPACITY` | Max requests for `/jobs/discover` per window | `5` |
| `RATE_LIMIT_DISCOVERY_REFILL_SECONDS` | Window duration for rate limiter refill | `60` |
| `INITIAL_ADMIN_EMAIL` | Email designated for ADMIN role upon registration | `admin@example.com` |
| `CORS_ALLOWED_ORIGINS` | Allowed origins for CORS | `http://localhost:5173,http://localhost:5174` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` or `prod`) | `default` |
| `COMPANY_CAREERS_ENABLED` | Enables Indian company career sources & public ATS feeds | `true` *(Active default)* |
| `REMOTIVE_ENABLED` | Enables Remotive secondary source (demoted) | `false` *(Disabled default)* |
| `NAUKRI_ENABLED` | Enables Naukri Enterprise Partner API | `false` *(Requires Partner API keys)* |
| `NAUKRI_CLIENT_ID` / `SECRET` | Naukri OAuth 2.0 Partner credentials | *(Required if NAUKRI_ENABLED=true)* |
| `FOUNDIT_ENABLED` | Enables Foundit (Monster India) Partner API | `false` *(Requires Partner API keys)* |
| `FOUNDIT_CLIENT_ID` / `SECRET` | Foundit OAuth 2.0 Partner credentials | *(Required if FOUNDIT_ENABLED=true)* |
| `INDEED_ENABLED` | Enables Indeed India Publisher/Partner API | `false` *(Requires Publisher ID)* |
| `INDEED_PUBLISHER_ID` | Indeed India Publisher ID | *(Required if INDEED_ENABLED=true)* |

---

## 🇮🇳 Phase 13 — India-First Job Discovery Platform

DOT Field prioritizes legitimate Indian job opportunities and explicit India-remote roles across popular India-relevant sources while strictly adhering to terms of service and avoiding unauthorized HTML scraping.

### Source Availability Matrix

| Source Adapter | Status Category | Access Method | Credentials Required |
| -------------- | --------------- | ------------- | -------------------- |
| **Company Career Pages** | `WORKING` | Public ATS & Structured Indian Employer Feeds (Razorpay, Swiggy, Flipkart, TCS, Infosys, Wipro) | None |
| **Naukri.com** | `DISABLED — PARTNER ACCESS REQUIRED` | Enterprise OAuth 2.0 / Recruiter API | `NAUKRI_CLIENT_ID`, `NAUKRI_CLIENT_SECRET` |
| **Foundit (Monster India)** | `DISABLED — PARTNER ACCESS REQUIRED` | Partner API | `FOUNDIT_CLIENT_ID`, `FOUNDIT_CLIENT_SECRET` |
| **Indeed India** | `DISABLED — PARTNER ACCESS REQUIRED` | Publisher / Partner API (`co=in`) | `INDEED_PUBLISHER_ID` |
| **Remotive** | `DISABLED` *(Demoted)* | Public API (Secondary source, filtered via `IndiaJobFilter`) | None |

### Location Normalization & India Job Filter

Every job listing passes through `IndiaJobFilter` and `IndiaLocationNormalizer` before extraction and persistence:
- **Indian Tech Hubs & Cities**: Bengaluru, Hyderabad, Chennai, Pune, Mumbai, Delhi NCR, Gurugram, Noida, Kolkata, Ahmedabad, Jaipur, Kochi, Chandigarh, Indore, etc. -> Classified as `isIndiaRelevant = true`, `normalizedCountry = "IN"`.
- **Explicit India Remote**: `"Remote - India"`, `"India - Remote"`, `"Remote (India)"`, `"Anywhere in India"` -> `isIndiaRelevant = true`, `isRemote = true`, `remoteCountry = "IN"`.
- **Ambiguous Remote / Foreign Locations**: Generic `"Remote"` without explicit country eligibility or foreign locations (`"San Francisco, USA"`, `"London, UK"`) are rejected (`isIndiaRelevant = false`) to prevent false positives.

## Active Profiles & Logging

- **Development (`SPRING_PROFILES_ACTIVE=dev`)**: Enables SQL log printing (`org.hibernate.SQL=DEBUG`) and binder parameter tracing (`BasicBinder=TRACE`).
- **Production (`SPRING_PROFILES_ACTIVE=prod`)**: Hardens logging defaults (`org.hibernate.SQL=WARN`, `BasicBinder=OFF`) to eliminate sensitive SQL parameter leakages.

```bash
# Run backend in development profile
SPRING_PROFILES_ACTIVE=dev .\mvnw.cmd spring-boot:run

# Run backend in production profile
SPRING_PROFILES_ACTIVE=prod .\mvnw.cmd spring-boot:run
```

---

## Database Migration & Concurrency-Safe Deduplication

- **Flyway**: Owns all database schema creation and structural migrations (`src/main/resources/db/migration/`).
- **Flyway Migration V6 (`V6__deduplication_fingerprint_unique_index.sql`)**:
  - Archives any pre-existing duplicate rows into `jobs_backup` table before enforcing partial unique index `ux_job_deduplication_fingerprint`.
  - Native atomic persistence in `JobDiscoveryPersistenceHelper` uses `JdbcTemplate` `INSERT ... ON CONFLICT (deduplication_fingerprint) DO UPDATE` to prevent race conditions during parallel ingestion runs.
  
> [!NOTE]
> **Production DBA Manual Indexing Step**:
> To avoid long table locks on high-traffic production databases, DB admins can create the index concurrently:
> ```sql
> -- 1. Detect duplicates
> SELECT deduplication_fingerprint, COUNT(*) FROM jobs WHERE deduplication_fingerprint IS NOT NULL GROUP BY 1 HAVING COUNT(*) > 1;
> -- 2. Run standalone dedupe script
> -- scripts/detect_and_dedupe_fingerprints.sql
> -- 3. Create index concurrently
> CREATE UNIQUE INDEX CONCURRENTLY ux_job_deduplication_fingerprint ON jobs (deduplication_fingerprint) WHERE deduplication_fingerprint IS NOT NULL;
> ```

---

## Rate Limiting & Security

- **Discovery Rate Limiter**: Endpoints `/jobs/discover` and `/jobs/ingestion/run` are protected by Bucket4j rate limiting using a composite client key (`userId` for authenticated users, client IP address for unauthenticated requests). Exceeding limit returns `HTTP 429 Too Many Requests` with a `Retry-After` header.
- **JWKS / RS256 Support**: Supports RS256 token verification via remote JWKS endpoints (`JWT_JWKS_URL`) or local RSA public keys with 15-minute TTL caching, falling back to HMAC (`JWT_SECRET`).

---

## Containerization & CI/CD

### Docker Build

```bash
docker build -t dotfield-backend -f dot-field-backend/Dockerfile dot-field-backend
docker run -p 8080:8080 -e DB_PASSWORD=your_pass -e JWT_SECRET=your_secret dotfield-backend
```

### GitHub Actions CI Workflow

The repository includes a GitHub Actions pipeline (`.github/workflows/ci.yml`) that automatically runs frontend static builds (`npm run build`) and backend unit test suites (`./mvnw test`) on pull requests and pushes to `main` or `feature/*` branches.

---

## Quick Start

### 1. Prerequisites

- Java 21 SDK
- Node.js 18+ & npm
- PostgreSQL 14+

### 2. Backend Setup

```bash
cd dot-field-backend

# Copy environment template
cp .env.example .env
# Edit .env to set your DB_PASSWORD and JWT_SECRET

# Run tests
.\mvnw.cmd clean test

# Run application
.\mvnw.cmd spring-boot:run
```

The backend server starts on `http://localhost:8080/api`.

### 3. Frontend Setup

```bash
# In root directory
npm install

# Run dev server
npm run dev
# → http://localhost:5173
```

---

## License

Private repository. All rights reserved.
