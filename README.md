# InvoiceFlow

Multi-tenant Order-to-Cash SaaS: customer, quotation, sales order, invoice, payment, allocation, ledger and
reporting, with configurable Indian GST.

This repository currently holds the **Phase 1 backend foundation**. Business features arrive in later phases.

## Layout

| Path | What it is |
| --- | --- |
| `backend/` | Spring Boot 4 / Spring Modulith application (Java 25, Maven wrapper) |
| `compose.yaml` | Local MongoDB, Redis and MinIO |
| `.github/workflows/backend.yml` | CI: `./mvnw verify` on JDK 25 |

## Running locally

Requirements: JDK 25 and Docker.

```bash
cd backend
./mvnw spring-boot:run      # starts ../compose.yaml automatically (dev profile)
./mvnw verify               # unit, architecture, Modulith and Testcontainers tests
```

On a machine with only JDK 21, add `-Djava.version=21`. The code uses no Java 25-only features yet, and CI
builds on 25.

Copy `.env.example` to `.env` to change local credentials. The MinIO console is at http://localhost:9001.
MinIO no longer publishes images to Docker Hub, so Compose uses the community-maintained `pgsty/minio`
build; set `MINIO_IMAGE` to use another.

## Backend modules

`com.invoiceflow.<module>`, verified by Spring Modulith (`ModularityTests`):

- `tenant`, `customer`, `catalog`, `invoicing`, `payment`, `ledger`: business modules, empty for now.
- `shared`: open shared kernel that every module may use and that depends on none of them.
  - `shared.money`: `Money`, a `BigDecimal` amount plus currency, never silently rounded.
  - `shared.tenancy`: `TenantContext` and `TenantScopedMongoOperations`, the only way to reach MongoDB.
  - `shared.security`: JWT resource server, the six roles and their hierarchy, `GET /api/v1/me`.
  - `shared.web`: `/api/v1` prefix and RFC 9457 problem-detail errors.
  - `shared.storage`: MinIO client configuration.

## Conventions

**Tenancy.** The tenant comes only from the access token's `tenant_id` claim; tokens without one are
rejected. Documents extend `TenantOwnedDocument`, and all reads, writes, counts and deletes go through
`TenantScopedMongoOperations`, which ANDs the tenant onto every filter and refuses cross-tenant writes.
`ArchitectureTests` fails the build if other code touches `MongoOperations` or Spring Data repositories.

**Security.** Every `/api/**` call needs a bearer token from the identity provider configured by
`JWT_ISSUER_URI`, carrying `tenant_id` and a `roles` array. Roles: `OWNER` > `ADMIN` > (`ACCOUNTANT`,
`SALES`) > `STAFF` > `VIEWER`, so `hasRole('VIEWER')` admits every member. Only health and info probes are
public.

**API.** Controllers declare paths without a prefix (`@GetMapping("/customers")`) and are served under
`/api/v1`. Errors are `application/problem+json` with a stable `type` such as
`urn:invoiceflow:problem:not-found`.

**Money.** Use `Money`, never `double` or `float` (enforced by `ArchitectureTests`). Amounts are stored in
MongoDB as `Decimal128` and sent over JSON as strings.

## Configuration

Profiles: `dev` (default, local Compose) and `prod` (all settings from the environment, ECS JSON logs).

| Variable | Purpose |
| --- | --- |
| `MONGODB_URI` | MongoDB connection string |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis |
| `JWT_ISSUER_URI` | OpenID Connect issuer that signs access tokens |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | Object storage |
