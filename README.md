# FreightLoom Backend

Multi-tenant load-management backend built with Java 21, Spring Boot 4, PostgreSQL, Flyway, and stateless JWT authentication.

## Tenant and identity model

`POST /api/v1/auth/signup` accepts email/password plus profile fields and either
a new `companyName` or an `existingCompanyId`. The backend calls Supabase Auth
using the configured project URL and publishable/anon key, then creates the
local user and company membership using the identity returned by Supabase.
Passwords are sent only to Supabase Auth and are never stored locally. When
email confirmation is enabled in Supabase, signup may return no access token;
the user must confirm their email and log in through Supabase before calling
other protected endpoints. New-company signups receive an active
`FLEET_MANAGER` membership. Existing-company requests create an invited
`DISPATCHER` membership and return `202 Accepted` until an active company
administrator approves it through
`PATCH /api/v1/company-memberships/{membershipId}/activate`.

Signup request example (no bearer token is needed):

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

```json
{
  "email": "dispatcher@example.com",
  "password": "a-strong-password",
  "firstName": "Casey",
  "lastName": "Driver",
  "companyName": "Haul Co."
}
```

For a join request, replace `companyName` with `existingCompanyId`. If Supabase
email confirmation is disabled, the response may contain an `accessToken`;
otherwise confirm the email and sign in through Supabase to obtain one.

Protected API requests require an issuer-signed `company_id` JWT claim. The
tenant filter verifies the local active user, company, and membership before
establishing request scope. The filter ignores client-supplied company IDs.
Because standard Supabase access tokens do not include `company_id`, configure
a Supabase access-token hook to add the selected company claim only after
checking the user's active membership. The migration also enables PostgreSQL
row-level security for company-owned tables; each transactional service sets
the transaction-local `app.current_company_id` after validating scope.

`UserAccount.auth_subject` stores the trusted identity provider's JWT `sub`.
Load status transitions are recorded append-only in `load_events`; monetary
values use `BigDecimal`/`NUMERIC` and carry explicit currency codes.

## Database migration

`V1__replace_core_schema_with_company_model.sql` is a destructive replacement
of the initial organization schema. It drops the old organizations, users,
memberships, loads, and load history and creates the company, fleet, load,
document, expense, safety, communications, and audit schema. Back up the
database before applying it.

For a fresh, empty PostgreSQL schema, run normally. If the disposable database
already contains tables but has no Flyway history table, baseline it at version
0 so Flyway applies V1:

```bash
export FLYWAY_BASELINE_ON_MIGRATE=true
export FLYWAY_BASELINE_VERSION=0
./mvnw spring-boot:run
```

Only enable this for the intended disposable schema: V1 drops existing core
tables. Leave `FLYWAY_BASELINE_ON_MIGRATE` unset or `false` for managed
databases that already have migration history. The migration is PostgreSQL
specific; the local H2 profile disables Flyway.

## Configuration

Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`,
`DB_SSL_MODE`, `JWT_ISSUER_URI`, `JWT_JWK_SET_URI`, `JWT_AUDIENCE`,
`SUPABASE_URL`, and `SUPABASE_ANON_KEY` in the runtime environment. Never
commit database credentials or a Supabase service-role key. Signup only needs
the project's publishable/anon key; the JWT settings must match that same
Supabase project.

```bash
export DB_HOST='your Supabase database or pooler host'
export DB_PORT='5432'
export DB_NAME='postgres'
export DB_USERNAME='your Supabase database username'
export DB_PASSWORD='your Supabase database password'
export DB_SSL_MODE='require'
export JWT_ISSUER_URI='your JWT issuer URI'
export JWT_JWK_SET_URI='your JWT JWK set URI'
export JWT_AUDIENCE='your JWT audience'
export SUPABASE_URL='https://your-project-ref.supabase.co'
export SUPABASE_ANON_KEY='your Supabase publishable or anon key'
./mvnw spring-boot:run
```

The `h2` profile provides a local file-backed database for development and
testing:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

## Test

```bash
./mvnw test
```
