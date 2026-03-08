# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EzPos platform console backend service — a Kotlin/Spring Boot 4 SaaS backend for managing merchants, subscriptions, client releases, data migrations, audit logging, and platform operations. Uses PostgreSQL, Redis (for opaque token auth), and Flyway migrations.

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ReleaseServiceTest

# Run the app (dev profile activates by default)
./mvnw spring-boot:run
```

**Local prerequisites**: PostgreSQL (database: `ezpos`), Redis on port 6379, Java 24.

**Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

## Architecture

**Kotlin 2.2 / Spring Boot 4.0.3 / Maven / Java 24**

### Feature-first package organization

Root package: `net.ezpos.console`

```
feature/
  auth/           # Login, token issuance (opaque token + Redis)
  user/           # Platform user CRUD, bootstrap (default admin)
  release/        # Client version management, rollout strategies, client updates
  merchant/       # Merchant CRUD (name, contact, address, enabled status)
  subscription/   # Plans and subscription management
  migration/      # Data migration task management
  audit/          # Audit log recording and querying
common/
  config/         # OpenApiConfig (Swagger/springdoc)
  security/       # OpaqueTokenAuthenticationFilter, TokenIntrospector/PrincipalLoader SPIs, CurrentPrincipalProvider
  entity/         # IdEntity base class, SnowflakeId generator
  exception/      # BusinessException hierarchy → ProblemDetail responses
  web/            # GlobalExceptionHandler (@RestControllerAdvice)
```

Each feature module follows the same internal structure: `controller/` → `service/` → `repository/` + `entity/`, with `dto/` and `mapper/` (MapStruct) for API contracts. Some modules also have a `model/` sub-package for domain logic (e.g., `SemVer`, `RolloutDecider` in release).

### Key architectural rules

- **Cross-module coupling**: modules call each other only through service-layer APIs, never directly accessing another module's repository or entity.
- **DTO boundary**: controllers never expose JPA entities; MapStruct mappers handle all DTO↔entity conversions.
- **Controller responsibility**: HTTP adaptation only (validation, auth annotations, call service, return DTO). No business logic in controllers.
- **Transaction boundaries**: owned by the service layer (`@Transactional`).

### Authentication

Stateless opaque token auth (no sessions). Bearer token in `Authorization` header → `OpaqueTokenAuthenticationFilter` → Redis lookup with sliding 24h TTL. Extension points: `TokenIntrospector` and `PrincipalLoader` SPIs in `common/security/spi/`. Services obtain the current user via `CurrentPrincipalProvider` in `common/security/current/`.

### User Bootstrap

`PlatformUserBootstrap` auto-creates a default admin user on startup (username: `admin`, password: `123456`). This ensures a usable login exists on first run.

### ID Generation

Snowflake IDs via `@SnowflakeId` annotation on entity fields. Configured in `application.yaml` (`snowflake.machine-id`, `snowflake.epoch`).

### Exception Handling

All business exceptions extend `BusinessException` and map to HTTP status codes via `GlobalExceptionHandler` using RFC 9457 `ProblemDetail`:
- `EntityNotFoundException` → 404
- `EntityAlreadyExistsException` → 409
- `BusinessRuleException` → 400
- `AuthenticationFailedException` → 401
- `DataIntegrityException` → 500

### Database

PostgreSQL with Flyway migrations in `src/main/resources/db/migration/`. JPA `ddl-auto: update` in dev, `validate` in prod — Flyway is the schema source of truth. Migration naming: `V<N>__<description>.sql`.

Current migrations:
- V1: Init schema (platform_users, console_release_applications, console_releases)
- V2: Client update reports
- V3: Merchants
- V4: Plans & subscriptions
- V5: Data migrations & audit logs

### Profiles

- **dev** (default): localhost PostgreSQL (`root`/`root`), `ddl-auto: update`, DEBUG SQL logging
- **prod**: DB credentials via env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), `ddl-auto: validate`, WARN logging

## Testing

Unit tests with JUnit 5 + MockK. Tests mirror the main source structure under `src/test/kotlin/`. Pattern: construct service with `mockk<T>()` dependencies, no Spring context needed for unit tests.

## Kotlin Compiler Flags

`-Xjsr305=strict` (strict nullability from Java annotations), `-Xannotation-default-target=param-property`. Compiler plugins: `spring`, `jpa`, `all-open` (opens `@Entity`, `@MappedSuperclass`, `@Embeddable` classes).

## Adding a New Feature Module

Follow the pattern in `feature/merchant/` (simplest) or `feature/release/` (most complex): create `controller/`, `service/`, `entity/`, `repository/`, `dto/`, `mapper/` sub-packages. Add `model/` if the module has non-trivial domain logic. See `docs/architecture/package-structure.md` for detailed conventions.

## API Endpoints

40 endpoints fully implemented across 9 controller groups (auth, users, release apps, releases, client updates, merchants, subscriptions, data migrations, audit). One endpoint (`POST /api/releases/{id}/artifact:presign`) returns 501 pending object storage integration. See `todo.md` for the full checklist.

## Key Documentation

- Product requirements: `PRD.md`
- Implementation checklist: `todo.md`
- Architecture & package conventions: `docs/architecture/package-structure.md`
- Auth system design: `docs/auth/README.md`
- Release module design: `docs/modules/release/README.md`
- Error response conventions: `docs/web/README.md`
