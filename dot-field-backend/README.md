# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.
> Phase 3 implements the Job Management subsystem.
> Phase 4 implements the Job Extraction & Ingestion subsystem.
> Phase 5 implements the Job Analysis & Matching subsystem.

---

## Requirements

| Tool       | Version |
|------------|---------|
| Java       | 21      |
| Maven      | 3.9+    |
| PostgreSQL | 14+     |

> **Note:** Maven is bundled via the Maven Wrapper (`mvnw` / `mvnw.cmd`), so a global Maven installation is not required.

---

## Setup

### 1. Clone the project

```bash
git clone <repository-url>
cd Dot-field-/dot-field-backend
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE dot_field;
```

### 3. Configure environment variables

Copy the example file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/dot_field
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

**On Windows**, set the environment variables before running:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/dot_field"
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
```

Or use your IDE's run configuration to inject them.

### 4. Run the application

```bash
# Unix / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The server starts on **http://localhost:8080**.

### 5. Test the health endpoint

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "data": {
    "status": "UP"
  },
  "message": "Success"
}
```

### 6. Run tests

Tests use an in-memory H2 database — no PostgreSQL required.

```bash
.\mvnw.cmd clean test
```

---

## Project Structure

```
dot-field-backend/
├── src/main/java/com/dotfield/
│   ├── DotFieldApplication.java    # Spring Boot entry point
│   ├── controller/                 # REST controllers (thin — delegate to services)
│   ├── service/                    # Business, extraction & matching services
│   ├── extractor/                  # JobExtractor Strategy, Normalization & DTOs
│   ├── matching/                   # Matching Engine: Normalization, Extractors, Matchers & Calculators
│   ├── repository/                 # Spring Data JPA repositories & specifications
│   ├── entity/                     # JPA entities & enums (database models)
│   ├── dto/                        # Data Transfer Objects & PagedResponse wrapper
│   ├── mapper/                     # Entity ↔ DTO conversion
│   ├── exception/                  # Global error handling
│   └── config/                     # Spring configuration beans
├── src/main/resources/
│   └── application.properties      # App configuration (uses env vars)
├── src/test/                       # Unit & Integration tests (JUnit 5 + Mockito + MockMvc)
├── pom.xml                         # Maven dependencies
├── .env.example                    # Environment variable template
└── .gitignore
```

---

## API Documentation — Phase 2 Candidate Profile

### Base Path: `/api`

### Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile` | Retrieve the complete candidate profile |
| `PUT`  | `/api/profile` | Create or update candidate profile basic details |

---

## API Documentation — Phase 3 Job Management

### Base Path: `/api`

### Job Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST`   | `/api/jobs` | Create a new job opportunity |
| `GET`    | `/api/jobs` | List jobs with pagination & composable filters |
| `GET`    | `/api/jobs/{id}` | Retrieve a single job opportunity by ID |
| `PUT`    | `/api/jobs/{id}` | Complete update of a job opportunity |
| `PATCH`  | `/api/jobs/{id}/status` | Update job status |
| `DELETE` | `/api/jobs/{id}` | Delete a job opportunity |

---

## API Documentation — Phase 4 Job Extraction & Ingestion

### Base Path: `/api`

### Extraction Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/jobs/extract` | Extract and ingest raw job listing data into Job domain model |

---

## API Documentation — Phase 5 Job Analysis & Matching

### Base Path: `/api`

### Matching Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/jobs/{id}/match` | Analyze candidate profile match against job opportunity |

---

### Matching Engine Architecture & Rules

- **Deterministic & Explainable:** Matching is 100% deterministic based on heuristic extraction, token normalization, and mathematical scoring. No LLMs or vector databases are used.
- **Scoring Range:** `0 <= overallScore <= 100` (integer rounded).
- **Match Categories:**
  - `80 – 100` → `STRONG_MATCH`
  - `60 – 79`  → `GOOD_MATCH`
  - `40 – 59`  → `PARTIAL_MATCH`
  - `0 – 39`   → `WEAK_MATCH`
- **Default Dimension Weights:**
  - Skill Score: **60%**
  - Experience Score: **20%**
  - Education Score: **10%**
  - Location/Remote Score: **10%**
- **Dynamic Weight Redistribution:**
  - If a dimension is `UNKNOWN` or `NOT_REQUIRED`, its weight is excluded from the denominator ($S_{\text{available}} = \sum_{d \in \text{Available}} W_d$) and remaining weights are normalized ($W_d^{\text{normalized}} = W_d / S_{\text{available}}$) so candidate scores are not penalized.
  - If all dimensions are `UNKNOWN`, `overallScore = 0` and `matchCategory = WEAK_MATCH`.
- **Dynamic On-Demand Calculation:** Match results are generated dynamically on demand and are not stored in the database.

#### Example Match Response (`GET /api/jobs/1/match`)

```json
{
  "data": {
    "jobId": 1,
    "profileId": 1,
    "overallScore": 88,
    "matchCategory": "STRONG_MATCH",
    "skillScore": 80,
    "experienceScore": 100,
    "educationScore": 100,
    "locationScore": 100,
    "matchedRequiredSkills": [
      "java",
      "spring boot"
    ],
    "missingRequiredSkills": [
      "postgresql"
    ],
    "matchedPreferredSkills": [
      "docker"
    ],
    "missingPreferredSkills": [
      "kubernetes"
    ],
    "experienceAnalysis": "Candidate has 4.0 years of total experience, meeting the required 3 years.",
    "educationAnalysis": "Candidate holds a degree matching the required Bachelor level.",
    "locationAnalysis": "Candidate location 'Bangalore, India' matches job location 'Bangalore, India'.",
    "strengths": [
      "Matches required skill: Java",
      "Matches required skill: Spring boot",
      "Matches preferred skill: Docker",
      "Candidate has 4.0 years of total experience, meeting the required 3 years.",
      "Candidate holds a degree matching the required Bachelor level.",
      "Candidate location 'Bangalore, India' matches job location 'Bangalore, India'."
    ],
    "gaps": [
      "Missing required skill: Postgresql",
      "Missing preferred skill: Kubernetes"
    ]
  },
  "message": "Job match analysis completed successfully"
}
```

---

## API Documentation — Phase 6 Resume Tailoring

### Base Path: `/api`

### Tailoring Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/jobs/{id}/resume/tailor` | Generate a job-specific tailored resume representation |

---

### Tailoring Engine Architecture & Constraints

- **Definition of Tailoring:** Resume Tailoring = selecting + prioritizing + ordering + emphasizing + job-keyword alignment + structured presentation.
- **Source-Traceability Rule:** Every factual statement in the tailored resume must trace directly to existing candidate profile fields. Unsupported information is omitted.
- **Anti-Fabrication Guarantee:** Zero invention of skills, metrics ("40% improvement"), achievements, job titles, technologies, or projects. Missing job skills are never inserted as candidate skills.
- **Deterministic Project Relevance Scoring Formula:**
  $$\text{projectScore} = (\text{matchedRequiredSkills} \times 3) + (\text{matchedPreferredSkills} \times 2) + (\text{otherMatchedKeywords} \times 1)$$
- **Experience & Bullet Prioritization:** Strict reverse chronological order across experiences. Intra-experience bullet lines are ranked by job keyword relevance while preserving exact original wording.
- **Non-Persisted & 100% Deterministic:** Derived on-demand dynamically without database persistence or external LLM/AI services. Same `Profile + Job` inputs always produce identical results.

#### Example Tailored Resume Response (`GET /api/jobs/1/resume/tailor`)

```json
{
  "data": {
    "jobId": 1,
    "profileId": 1,
    "summary": "Software Engineer with experience in Java, Spring Boot. Previously worked at Acme Corp.",
    "skills": {
      "primary": [
        "Java",
        "Spring Boot"
      ],
      "secondary": [
        "Git"
      ]
    },
    "experience": [
      {
        "id": 10,
        "company": "Acme Corp",
        "role": "Software Engineer",
        "description": "Developed backend APIs using Java.\nIntegrated PostgreSQL database.",
        "startDate": "2022-01-01",
        "endDate": null,
        "emphasized": true,
        "matchingKeywords": [
          "java",
          "postgresql"
        ]
      }
    ],
    "education": [
      {
        "id": 5,
        "institution": "State University",
        "degree": "Bachelor of Science",
        "fieldOfStudy": "Computer Science",
        "startDate": "2018-09-01",
        "endDate": "2022-05-31",
        "grade": "3.8",
        "emphasized": true
      }
    ],
    "projects": [
      {
        "id": 20,
        "name": "Backend API Service",
        "description": "RESTful service built with Spring Boot",
        "githubUrl": "https://github.com/candidate/api",
        "liveUrl": null,
        "technologies": [
          "Java",
          "Spring Boot"
        ],
        "projectScore": 5,
        "emphasized": true,
        "matchingKeywords": [
          "java",
          "spring boot"
        ]
      }
    ],
    "links": [
      {
        "type": "GitHub",
        "url": "https://github.com/candidate"
      }
    ],
    "tailoringAnalysis": {
      "emphasizedSkills": [
        "Java",
        "Spring Boot"
      ],
      "emphasizedExperiences": [
        "Software Engineer at Acme Corp"
      ],
      "emphasizedProjects": [
        "Backend API Service"
      ],
      "matchedKeywords": [
        "java",
        "spring boot",
        "postgresql"
      ],
      "unusedJobKeywords": [
        "aws",
        "kubernetes"
      ],
      "tailoringNotes": "Emphasized 2 primary skills, 1 experience entries, and 1 projects."
    }
  },
  "message": "Resume tailored successfully"
}
```

---

## API Response Format

### Success

```json
{
  "data": { ... },
  "message": "Success"
}
```

### Error

```json
{
  "status": 404,
  "message": "Resource not found",
  "timestamp": "2026-08-30T12:00:00"
}
```

---

## Current Phase

```
Phase 6 — Resume Tailoring
Status: Complete
```

### What's included in Phase 6

- ✅ Reused Phase 2 Candidate `Profile` & Phase 3 `Job` domain entities without duplicate models
- ✅ Reused Phase 5 requirement extraction (`JobRequirementExtractor`) & normalization (`SkillNormalizationUtil`)
- ✅ 100% deterministic tailoring engine (`ResumeTailoringEngine`)
- ✅ Precise keyword selector separating matched vs unused keywords (`ResumeKeywordSelector`)
- ✅ Primary vs. secondary skills builder with zero missing-skill insertion (`ResumeSectionBuilder`)
- ✅ Strict reverse chronological experience ordering with intra-experience bullet prioritization (`ResumeExperiencePrioritizer`)
- ✅ Deterministic project relevance scoring formula & stable tie-breaking (`ResumeSectionBuilder`)
- ✅ Education & social link extraction preserving factual candidate data
- ✅ Strict source-traceable summary generator (`ResumeSummaryGenerator`)
- ✅ Explainability metadata breakdown in `tailoringAnalysis`
- ✅ Dynamic on-demand orchestration service (`ResumeTailoringService`)
- ✅ `GET /api/jobs/{id}/resume/tailor` REST endpoint in `ResumeTailoringController`
- ✅ Adversarial anti-fabrication test suite (missing skills, title inflation, unsupported tech, fake metrics, fake achievements, fake projects)
- ✅ Determinism test suite & empty profile safety tests
- ✅ 134 total passing automated tests across Phase 1, 2, 3, 4, 5, and 6

---

## Future Phases

| Phase | Focus                     | Status       |
|-------|---------------------------|--------------|
| 1     | Backend Foundation        | ✅ Complete  |
| 2     | Candidate Profile         | ✅ Complete  |
| 3     | Job Management            | ✅ Complete  |
| 4     | Job Extraction & Ingestion| ✅ Complete  |
| 5     | Job Analysis & Matching   | ✅ Complete  |
| 6     | Resume Tailoring          | ✅ Complete  |

---

## License

Private project — not for distribution.
