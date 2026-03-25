# Configuration reference

Configuration is layered: **local `application.yml`** in each service, plus **Spring Cloud Config** from Git when `CONFIG_SERVER_URL` points to a running config server.

## Environment variables (Docker Compose)

Set these via a `.env` file in the project root (Compose loads it automatically). **Do not commit real secrets.**

### Databases

| Variable | Purpose |
|----------|---------|
| `AUTH_POSTGRES_DB`, `AUTH_POSTGRES_USER`, `AUTH_POSTGRES_PASSWORD` | Auth service PostgreSQL |
| `ORDERS_POSTGRES_DB`, `ORDERS_POSTGRES_USER`, `ORDERS_POSTGRES_PASSWORD` | Orders service PostgreSQL |

### Eureka

| Variable | Purpose |
|----------|---------|
| `EUREKA_USER`, `EUREKA_PASSWORD` | Basic auth for the Eureka HTTP API |

### OAuth2 and admin (auth service)

| Variable | Purpose |
|----------|---------|
| `SERVICE_CLIENT_ID`, `SERVICE_CLIENT_SECRET` | Registered OAuth2 client (service) |
| `EXTERNAL_CLIENT_ID`, `EXTERNAL_CLIENT_SECRET` | Registered OAuth2 client (external) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Admin user for secured API routes |

### Config server

| Variable | Purpose |
|----------|---------|
| `CONFIG_GIT_URI` | Git URI for config repository |
| `CONFIG_GIT_LABEL` | Branch or label (e.g. `main`) |
| `SSL_KEY_STORE_PASSWORD` | Password for the config server PKCS12 keystore |

### Email (orders)

| Variable | Purpose |
|----------|---------|
| `MAIL_HOST`, `MAIL_PORT` | SMTP server |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP credentials |
| `NOTIFICATION_ENABLED` | When `true`, sends order confirmation emails |
| `NOTIFICATION_FROM_EMAIL` | From address |

### Observability (auth & orders)

| Variable | Purpose |
|----------|---------|
| `ZIPKIN_ENDPOINT` | Zipkin HTTP collector URL (e.g. `http://zipkin:9411/api/v2/spans` in Compose) |
| `ZIPKIN_ENABLED` | Set to `false` to disable exporting spans to Zipkin while keeping tracing IDs in logs |
| `TRACING_SAMPLE_PROBABILITY` | Sampling ratio `0.0`–`1.0` (use lower values in production) |

### TLS to config server (applications)

Applications trust the config server certificate using:

- `CONFIG_TLS_ENABLED`
- `CONFIG_TLS_TRUST_STORE`, `CONFIG_TLS_TRUST_STORE_PASSWORD`

Compose mounts `ssl/truststore.p12` into auth and orders containers.

## Actuator (orders)

Exposed endpoints include `health`, `info`, and `circuitbreakers`. Secure non-health endpoints appropriately in production (see `SecurityConfig`).

## Resilience4j (orders)

- **OpenFeign**: `spring.cloud.openfeign.circuitbreaker.enabled=true` — Feign clients use circuit breakers; configure per-client instances under `resilience4j.circuitbreaker.instances`.
- **Email**: Instance name `emailSend` wraps `JavaMailSender` with a fallback that logs on open circuit or failure.
