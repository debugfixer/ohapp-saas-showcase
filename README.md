# OHAPP SaaS Showcase

Backend showcase of a SaaS platform for automating occupational health and related HR-medical workflows.

This repository demonstrates practical Java backend development with a focus on REST API design, business logic implementation, persistence, security, validation, documentation, and testing.

> **Note**
> This is a **demonstration version** of the project prepared for code review and portfolio purposes.  
> It is **not intended to be run as a production-ready or plug-and-play application** and may contain reduced configuration, trimmed setup, or simplified project parts for showcase purposes.

---

## Overview

OHAPP is a backend-oriented SaaS application built around real business scenarios such as employee management, medical examination tracking, referrals, event calendar workflows, audit history, and data import/export.

The goal of this showcase repository is to present the backend architecture, API approach, persistence design, and testing practices used in the project in a concise and review-friendly format.

---

## Business domain

The platform is focused on automating occupational health and related HR-medical processes, including:

- employee management
- medical examination workflows
- referral generation and tracking
- referral templates
- calendar-related workflows
- audit and change history
- import and export of operational data

---

## Key features

- REST API for applied business workflows
- Employee and medical examination management
- Due-date related scenarios
- Referral and template handling
- Calendar export in `.ics` format
- Audit/change tracking
- Excel and CSV import/export
- Multitenancy for organization-level data isolation
- API documentation with OpenAPI / Swagger UI
- Backend testing with JUnit 5, Spring Boot Test, and MockMvc

---

## Tech stack

- Java 21
- Spring Boot 3.3.4
- Spring Web
- REST API
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Spring Security
- JWT (JJWT)
- Bean Validation
- OpenAPI / Swagger UI
- Apache POI
- OpenCSV
- Lombok
- Maven

---

## Architecture

The backend follows a layered structure with separation of responsibilities across:

- controller
- service
- repository
- model
- dto
- security
- config
- infra
- notify
- support
- tools
- web-related layers

This structure was designed to improve readability, maintainability, and support further product evolution beyond a simple prototype.

---

## Testing

The project includes backend-oriented tests that cover both application startup and isolated API behavior.

### Implemented testing approaches

- Smoke/context tests with Spring Boot Test
- Isolated API checks with standalone MockMvc
- Due API response validation
- Calendar export response validation
- Minimal end-to-end style flow for selected API scenarios

### Example tested scenarios

**Due API**
- `GET /api/due`
- Verifies HTTP 200 response
- Verifies JSON response handling

**Calendar export**
- `POST /api/calendar/export`
- Verifies HTTP 200 response
- Verifies `text/calendar` content type
- Verifies `.ics` payload generation

---

## What this repository demonstrates

This repository is intended to demonstrate:

- practical Java / Spring Boot backend development
- REST API design for real business workflows
- persistence layer implementation with JPA / Hibernate
- PostgreSQL integration
- database schema migration management with Flyway
- application security with Spring Security and JWT
- backend testing with JUnit 5 and MockMvc
- maintainable service-oriented backend structure

---

## Project status

This repository represents a production-minded MVP that reached trial usage on test clients and real user scenarios.

At the same time, this GitHub version is intentionally published as a **showcase repository** rather than a fully reproducible commercial project. Some configuration, environment-specific setup, integrations, or internal project details may be reduced or omitted.

---

## Author

**Ivan Gurev**  
Java Backend Developer

Focus areas:
- Java backend development
- Spring Boot and REST API
- PostgreSQL, JPA, Hibernate
- Spring Security and JWT
- Backend testing with JUnit and MockMvc
