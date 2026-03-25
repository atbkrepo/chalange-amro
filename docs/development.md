# Development

## Prerequisites

- **JDK 21** (see each module’s `build.gradle.kts` `toolchain`).
- **Docker** (optional, for Compose-based full stack).
- **Gradle**: each module ships its own wrapper (`gradlew` / `gradlew.bat`).

## Repository layout

```
chalage-abn-amro/
├── auth/          # OAuth2 authorization server
├── orders/        # Orders API (resource server)
├── config/        # Spring Cloud Config Server
├── discovery/     # Eureka server
├── ssl/           # Truststore for clients talking to HTTPS config server
├── docker-compose.yml
└── docs/
```

## Build and test

From each module directory:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

Integration tests may require **config server** and **TLS** to be available; if `contextLoads` fails with config client or SSL errors, run tests with a test profile that disables config import or points to a mock server, or run against a real local stack.

## Run order (local / Compose)

1. **Discovery** (Eureka) — port **8761**.
2. **Config server** — depends on Eureka; HTTPS port **8888** (with provided keystore).
3. **Auth** and **orders** databases (PostgreSQL).
4. **Auth** application — default HTTP port **9000** in Compose.
5. **Orders** application — default HTTP port **8080** in Compose.
6. **Zipkin** — UI **9411** (optional for tracing).

Compose wires `ZIPKIN_ENDPOINT` for auth and orders.

## API documentation

- **Swagger UI**: exposed by auth and orders (paths such as `/swagger-ui.html` and `/v3/api-docs` per each app’s configuration).

## Config Git repository

The config server clones `CONFIG_GIT_URI` and serves files under `{application}/` for `spring.application.name`. Ensure repo access (SSH key mounted in Compose for `config-server`).
