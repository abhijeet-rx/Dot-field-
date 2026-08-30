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

| Variable | Description | Example / Required |
|----------|-------------|-------------------|
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/dot_field` |
| `DB_USERNAME` | PostgreSQL database user | `postgres` |
| `DB_PASSWORD` | PostgreSQL database password | *(Required — no hardcoded fallback)* |
| `JWT_SECRET` | Secret key for JWT signing | *(Required — min 32 characters / 256 bits)* |
| `JWT_EXPIRATION` | Token expiration time in milliseconds | `86400000` (24h) |
| `INITIAL_ADMIN_EMAIL` | Email designated for ADMIN role upon registration | `admin@example.com` |
| `CORS_ALLOWED_ORIGINS` | Allowed origins for CORS | `http://localhost:5173,http://localhost:5174` |

---

## Database Migration & JPA Architecture

- **Flyway**: Owns all database schema creation and structural migrations (`src/main/resources/db/migration/`).
- **Hibernate DDL Auto**: Configured to `validate` in production (`spring.jpa.hibernate.ddl-auto=validate`). Hibernate validates schema entity mappings against Flyway migrations without altering the database.
- **Test Database**: Unit and integration tests run strictly against an isolated in-memory H2 database (`spring.jpa.hibernate.ddl-auto=create-drop`, Flyway disabled for test speed).

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

## Authentication & Security

- **Stateless JWT Security**: Requests to protected routes require `Authorization: Bearer <token>`.
- **Environment-Only Secrets**: `jwt.secret` is loaded from environment variables (`JWT_SECRET`) without fallback.
- **Data Isolation**: Candidate profiles, skills, experience, education, projects, matching analyses, and tailored resumes are strictly isolated per authenticated user ID.
- **Role-Based Access Control (RBAC)**:
  - `USER`: Browse jobs, calculate match scores, tailor resumes, manage personal profile.
  - `ADMIN`: Job ingestion, manual creation, status modification, and automated discovery triggers (`POST /api/jobs`, `POST /api/jobs/discover`, `POST /api/jobs/extract`, `PUT /api/jobs/{id}`, `PATCH /api/jobs/{id}/status`, `DELETE /api/jobs/{id}`).

---

## License

Private repository. All rights reserved.
