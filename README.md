# FreightLoom Backend

Initial multi-tenant load-management backend structure.

- Java 21
- Spring Boot 4.1.2
- PostgreSQL with Flyway-managed schema
- Spring Security OAuth2 resource server with stateless JWTs

The first migration creates organizations, users, organization memberships, loads,
and append-only load status history. Load lookups require an organization ID in
the repository query; callers must take it from `TenantContext`, never from a
request body. `LoadStatusService` derives the tenant from that context and
atomically records status changes and their history.

## Configuration

Set `DB_USERNAME`, `DB_PASSWORD`, `JWT_ISSUER_URI`, `JWT_JWK_SET_URI`, and
`JWT_AUDIENCE` in the runtime environment. Authenticated access tokens must be
signed by the configured issuer, target the configured audience, and include
the `organization_id` claim. The tenant filter maps the signed `sub` claim to
an active local user and verifies active organization membership on each
request. Client-supplied organization IDs are not used to establish scope.

## Run

```bash
./mvnw spring-boot:run
```

For local development, provide a PostgreSQL database and the configuration
above. Hibernate validates the schema created by Flyway; it does not generate
or modify the production schema.

## Test

```bash
./mvnw test
```
