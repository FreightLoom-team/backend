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

Self-signup at `POST /api/v1/auth/signup` creates a carrier with an active
`FLEET_MANAGER` membership or requests to join an existing carrier with an
`INVITED` `DISPATCHER` membership. Existing-carrier signups return `202 Accepted`
and cannot access tenant data until an administrator activates the membership;
the tenant filter only admits active members. An active `OWNER`, `ADMIN`, or
`FLEET_MANAGER` can approve a pending membership with
`PATCH /api/v1/organization-members/{membershipId}/activate`; the tenant is
derived from the authenticated session, not a request parameter.

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
