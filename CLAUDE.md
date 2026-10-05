# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ShopSphere is a Spring Boot / Spring Cloud microservices system (Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3) built as a Maven multi-module reactor project. Four modules:

- **discovery-server** (port 8761) — Eureka service registry. Services register here; the gateway resolves `lb://service-name` through it.
- **config-server** (port 8888) — Spring Cloud Config Server running in `native` profile, serving YAML files from `config-server/src/main/resources/configs/` (not a Git-backed config repo). Each client file is named `<spring.application.name>.yml` (e.g. `api-gateway.yml`, `product-service.yml`), plus a shared `application.yml`.
- **api-gateway** (port 8080) — Spring Cloud Gateway (WebFlux/Netty, reactive). Single entry point for clients. Owns CORS, request routing to backend services via Eureka, and Redis-backed rate limiting.
- **product-service** (port 8082) — the only business service so far. Spring MVC (servlet, not reactive), JPA/Postgres persistence, Flyway migrations, Redis response caching, OpenAPI/Swagger docs.

Startup order matters: **discovery-server → config-server → product-service / api-gateway**. Both product-service and api-gateway import config from the Config Server at boot (`spring.config.import: optional:configserver:...`) and register with Eureka.

## Common Commands

All commands run from the repo root using `mvn` against the reactor `pom.xml`, which aggregates the four modules. (No `mvnw` wrapper script is checked in — only an empty `.mvn/` dir — so Maven must be installed locally.)

```bash
# Build everything
mvn clean install

# Build/test a single module (and only the modules it depends on)
mvn -pl product-service -am clean install

# Run all tests in the repo
mvn test

# Run tests for one module
mvn -pl product-service test

# Run a single test class
mvn -pl product-service test -Dtest=ProductCachingTest

# Run a single test method
mvn -pl product-service test -Dtest=ProductCachingTest#getById_populatesCacheOnFirstRead

# Run a service locally (repeat per module, in dependency order)
mvn -pl discovery-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Infrastructure (Postgres + Redis) needed by product-service comes from `docker-compose.yml`:

```bash
docker compose up -d   # product-db on host port 5433, redis on 6379
```

Note: `docker-compose.yml` maps Postgres to host port **5433**, but `product-service`'s default `application.yml` datasource URL defaults to port **5432**. When running via Docker Compose, override `DB_URL` (e.g. `DB_URL=jdbc:postgresql://localhost:5433/product_db`) or adjust one of the two.

product-service's tests (`ProductRepositoryTest`, `ProductCachingTest`) use Testcontainers to spin up ephemeral Postgres/Redis containers automatically — Docker must be running, but `docker-compose up` is not required for tests. Test-specific config lives in `product-service/src/test/resources/application.properties`, which disables Config Server/Eureka lookups during tests.

## Architecture Notes

### Config propagation
Services don't hardcode most settings — they pull from config-server at startup. When changing a service's routing, CORS, rate-limit, or logging config, check whether the authoritative copy lives in `config-server/src/main/resources/configs/<service>.yml` rather than the service's own `application.yml` (the service's local `application.yml` is often just the `spring.application.name` + `spring.config.import` pointer, or env-driven overrides for local/standalone runs). `api-gateway`'s routing, CORS, and rate-limiter settings are defined in `config-server/.../configs/api-gateway.yml`, not in `api-gateway/src/main/resources/application.yml`.

### Correlation IDs
Every request gets a correlation ID threaded through logs across the gateway and downstream services:
- `api-gateway`'s `CorrelationIdGlobalFilter` (highest precedence `GlobalFilter`) validates/generates the ID (`X-Correlation-Id` header, must match `^[A-Za-z0-9-]{1,64}$` to block log injection), attaches it to the forwarded request and response, and logs one summary line per request.
- `product-service`'s `CorrelationIdFilter` (a servlet `OncePerRequestFilter`, highest precedence) reads that header (or generates its own ID if called directly, bypassing the gateway) and puts it in SLF4J's MDC so the log pattern (`%5p [%X{correlationId:-}]`, defined in `config-server/.../configs/application.yml`) prints it on every line.
- If you add a new service, replicate this filter pattern to keep traces linkable.

### Rate limiting
`api-gateway` uses Spring Cloud Gateway's Redis-backed `RequestRateLimiter` filter per-route (see `api-gateway.yml`). The bucket key comes from `RateLimiterConfig.ipKeyResolver()`, which keys by client IP — noted in the code as a placeholder until per-user auth exists (it won't work correctly behind a load balancer without reading `X-Forwarded-For`).

### product-service layering
Standard layered structure: `controller` → `service` → `repository`, with `mapper` (static methods, no mapping framework) converting entities to DTOs (`dto` package, Java records for request/response). `model.AuditableEntity` is a `@MappedSuperclass` providing `createdAt`/`updatedAt` via Spring Data JPA auditing (enabled in `config.JpaAuditingConfig`). `Product` uses optimistic locking (`@Version`); concurrent update conflicts surface as HTTP 409 via `GlobalExceptionHandler`.

Schema changes go through Flyway (`src/main/resources/db/migration/V*__*.sql`), and `spring.jpa.hibernate.ddl-auto` is `validate` — Hibernate never auto-generates schema, so every entity change needs a corresponding migration.

### Caching
Reads (`ProductService.getById`) are cached in Redis via `@Cacheable` (`CacheConfig.PRODUCT_CACHE`, 10-minute TTL, JSON serialization, key-prefixed `shopsphere:product-service:`). Writes (`update`, `delete`) evict the cache entry for that ID; `create` does not populate it. Cache errors are logged, not thrown (`LoggingCacheErrorHandler`) — Redis being down should degrade to DB reads, not break the API.

### Error handling
`product-service` uses a single `@RestControllerAdvice` (`GlobalExceptionHandler`) returning RFC 7807 `ProblemDetail` responses for not-found, bean/param validation, type mismatches, optimistic-lock conflicts, and a catch-all 500. Follow this pattern (don't throw raw exceptions or build ad hoc error bodies) when adding new endpoints or exception types.

### API docs
`product-service` exposes Swagger UI at `/swagger-ui.html` (springdoc-openapi). Endpoints are tagged under "Products" (`@Tag` on `ProductController`).