# DOT Field — Backend

> DOT Field is a personal job discovery and resume-tailoring platform.
> Phase 1 establishes the backend foundation using Spring Boot and PostgreSQL.
> Phase 2 implements the Candidate Profile subsystem.

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
│   ├── service/                    # Business logic layer
│   ├── repository/                 # Spring Data JPA repositories
│   ├── entity/                     # JPA entities (database models)
│   ├── dto/                        # Data Transfer Objects (API boundaries)
│   ├── mapper/                     # Entity ↔ DTO conversion
│   ├── exception/                  # Global error handling
│   └── config/                     # Spring configuration beans
├── src/main/resources/
│   └── application.properties      # App configuration (uses env vars)
├── src/test/                       # Tests (JUnit 5 + Spring Boot Test)
├── pom.xml                         # Maven dependencies
├── .env.example                    # Environment variable template
└── .gitignore
```

### Architecture

```
Controller → Service → Repository → PostgreSQL
```

- **Controllers** are thin — they validate input and delegate to services.
- **Services** contain all business logic.
- **Repositories** handle persistence only.
- **DTOs** are used at API boundaries; JPA entities are never exposed directly.

---

## API Documentation — Phase 2 Candidate Profile

### Base Path: `/api`

### Profile Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile` | Retrieve the complete candidate profile |
| `PUT`  | `/api/profile` | Create or update candidate profile basic details |

#### Update Profile Request Example (`PUT /api/profile`)

```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "linkedinUrl": "https://linkedin.com/in/janedoe",
  "githubUrl": "https://github.com/janedoe",
  "portfolioUrl": "https://janedoe.dev"
}
```

---

### Skills Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/skills` | List candidate skills |
| `POST` | `/api/profile/skills` | Add a new skill to candidate profile |
| `DELETE` | `/api/profile/skills/{id}` | Delete a skill |

#### Add Skill Request Example (`POST /api/profile/skills`)

```json
{
  "name": "Java",
  "category": "LANGUAGE"
}
```

Categories: `LANGUAGE`, `FRONTEND`, `BACKEND`, `DATABASE`, `TOOL`, `FRAMEWORK`, `OTHER`

---

### Education Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/education` | List education records |
| `POST` | `/api/profile/education` | Add education record |
| `PUT`  | `/api/profile/education/{id}` | Update education record |
| `DELETE` | `/api/profile/education/{id}` | Delete education record |

#### Add Education Request Example (`POST /api/profile/education`)

```json
{
  "institution": "MIT",
  "degree": "Bachelor of Science",
  "fieldOfStudy": "Computer Science",
  "startDate": "2018-09-01",
  "endDate": "2022-05-30",
  "grade": "3.9 GPA"
}
```

---

### Project Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/projects` | List candidate projects |
| `POST` | `/api/profile/projects` | Add project record |
| `PUT`  | `/api/profile/projects/{id}` | Update project record |
| `DELETE` | `/api/profile/projects/{id}` | Delete project record |

#### Add Project Request Example (`POST /api/profile/projects`)

```json
{
  "name": "DOT Field Backend",
  "description": "Job discovery and resume tailoring backend",
  "githubUrl": "https://github.com/example/dot-field",
  "liveUrl": "https://dotfield.dev",
  "technologies": ["Java 21", "Spring Boot", "PostgreSQL"]
}
```

---

### Experience Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/profile/experience` | List candidate work experience |
| `POST` | `/api/profile/experience` | Add experience record |
| `PUT`  | `/api/profile/experience/{id}` | Update experience record |
| `DELETE` | `/api/profile/experience/{id}` | Delete experience record |

#### Add Experience Request Example (`POST /api/profile/experience`)

```json
{
  "company": "Acme Corp",
  "role": "Senior Software Engineer",
  "description": "Led backend platform architecture and microservices design",
  "startDate": "2022-06-01",
  "endDate": "2024-08-15"
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
  "timestamp": "2025-01-01T12:00:00"
}
```

### Validation Error

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2025-01-01T12:00:00",
  "errors": {
    "email": "must not be blank",
    "name": "size must be between 2 and 100"
  }
}
```

---

## Current Phase

```
Phase 2 — Candidate Profile
Status: Complete
```

### What's included in Phase 2

- ✅ Candidate `Profile`, `Skill`, `Education`, `Project`, and `Experience` JPA entities & database tables
- ✅ JPA relationships (`@OneToMany`, `@ManyToOne`, `@ElementCollection`) with cascade and orphan removal
- ✅ Skill category classification (`SkillCategory` enum)
- ✅ DTO layer (`ProfileResponse`, `UpdateProfileRequest`, `SkillRequest`, `SkillResponse`, `EducationRequest`, `EducationResponse`, `ProjectRequest`, `ProjectResponse`, `ExperienceRequest`, `ExperienceResponse`)
- ✅ Bean Validation for all input DTOs
- ✅ Date range validation (`startDate <= endDate`)
- ✅ Case-insensitive duplicate skill validation
- ✅ Spring Data JPA Repositories
- ✅ Mapper layer (`ProfileMapper`)
- ✅ Business logic services (`ProfileService`, `SkillService`, `EducationService`, `ProjectService`, `ExperienceService`)
- ✅ Thin REST Controllers (`ProfileController`, `SkillController`, `EducationController`, `ProjectController`, `ExperienceController`)
- ✅ Global exception handling for `BadRequestException` and `ResourceNotFoundException`
- ✅ Complete integration test suite (21 passing tests)

---

## Future Phases

| Phase | Focus                     |
|-------|---------------------------|
| 3     | Job Management            |
| 4     | Job Extraction            |
| 5     | Job Analysis & Matching   |
| 6     | Resume Tailoring          |

These phases will be implemented incrementally in future iterations.

---

## License

Private project — not for distribution.
