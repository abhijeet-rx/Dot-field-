# DOT Field — Job Intelligence Platform

> DOT Field is an AI-powered job discovery, requirement analysis, fit matching, candidate data protection, and resume tailoring platform.

## Overview

DOT Field helps candidates discover relevant job opportunities, analyze job requirements against their authentic candidate profile, score job fit, generate tailored resumes, and manage job opportunities — while preserving candidate privacy and manual application workflows.

---

## Tech Stack

- **Frontend**: React 19, Vite, React Router DOM, CSS3 (Glassmorphism & Micro-animations)
- **Backend**: Java 21, Spring Boot 3.4.1 (Web, Data JPA, Security, Validation, Flyway)
- **Security**: Spring Security 6, JJWT (0.12.6), BCrypt Password Hashing, Stateless Sessions
- **Database**: PostgreSQL (Production/Dev), H2 (In-Memory Unit Testing)

---

## Quick Start

### 1. Prerequisites

- Java 21 SDK
- Node.js 18+ & npm
- PostgreSQL 14+ (or run locally using in-memory H2 fallback mode)

### 2. Backend Setup

```bash
cd dot-field-backend

# Copy environment template
cp .env.example .env

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
- **Environment-Only Secrets**: `jwt.secret` is loaded from environment variables (`JWT_SECRET`).
- **Data Isolation**: Candidate profiles, skills, experience, education, projects, matching analyses, and tailored resumes are isolated per authenticated user.
- **Role-Based Access Control (RBAC)**:
  - `USER`: Browse jobs, calculate match scores, tailor resumes, manage personal profile.
  - `ADMIN`: Job ingestion, manual creation, status modification, and automated discovery triggers (`POST /api/jobs`, `POST /api/jobs/discover`, `POST /api/jobs/extract`).

---

## License

Private repository. All rights reserved.
