# Challenge ABN AMRO — distributed services

Spring Boot **4** microservices for **orders** and **OAuth2 authentication**, with **Eureka** discovery, **Spring Cloud Config** (Git-backed), **distributed tracing** (Micrometer + Zipkin), and **Resilience4j** circuit breaking on the orders service.

## Modules

| Directory | Application | Responsibility |
|-----------|-------------|----------------|
| `discovery/` | Eureka | Service registry |
| `config/` | Config Server | Central YAML from Git (HTTPS) |
| `auth/` | Authorization Server | OAuth2, users, clients (PostgreSQL) |
| `orders/` | Resource server | Products, orders, inventory, mail notifications (PostgreSQL) |

## Quick start (Docker Compose)

1. Copy `.env.example` to `.env` and set **passwords and secrets** (see [docs/configuration-reference.md](docs/configuration-reference.md)). Do not commit `.env`.
2. Ensure **SSL assets** exist where Compose expects them (`config/ssl/config-server.p12`, `ssl/truststore.p12`, SSH key for config Git if used).
3. From the repository root:

```bash
docker compose up --build
```

### Default ports (host)

| Service | Port |
|---------|------|
| Eureka | 8761 |
| Config Server | 8888 (HTTPS) |
| Auth | 9000 |
| Orders | 8080 |
| Auth PostgreSQL | 5432 |
| Orders PostgreSQL | 5433 |
| Zipkin | 9411 |

Open **Zipkin** at [http://localhost:9411](http://localhost:9411) to inspect traces from auth and orders.

## Technology stack

- **Java 21**, **Gradle** (Kotlin DSL) per module  
- **Spring Boot 4**, **Spring Cloud** (Config, Netflix Eureka, OpenFeign)  
- **PostgreSQL**  
- **Micrometer Tracing** + **Zipkin** (correlation IDs in logs: `[app,traceId,spanId]`)  
- **Resilience4j** circuit breakers (OpenFeign + email `emailSend` instance)  
- **SpringDoc OpenAPI** (Swagger UI)

## Documentation

Detailed material lives under **[docs/](docs/README.md)**:

- [Architecture](docs/architecture.md) — components and request flow  
- [Configuration reference](docs/configuration-reference.md) — environment variables  
- [Development](docs/development.md) — build, layout, run order  

## Building without Docker

Each module is a standalone Gradle project:

```bash
cd orders
./gradlew build
```

Use the same pattern for `auth`, `config`, and `discovery`.
