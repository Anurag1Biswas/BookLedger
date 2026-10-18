# Security design (JWT, stateless)

```
Request
  └─ JwtAuthFilter            reads "Authorization: Bearer <jwt>"
       ├─ JwtService          verifies signature + expiry, reads the username
       ├─ AdminUserDetailsService   loads the user (and role) from admin_users on EVERY request
       └─ sets the SecurityContext  (authority = "ADMIN" or "LIBRARIAN")
  └─ SecurityConfig rules     401 if not authenticated, 403 if the role is not allowed
```

| Class | Package | Responsibility |
|---|---|---|
| `SecurityConfig` | `config` | Filter chain, URL rules, CORS, password encoder, `AuthenticationProvider`, `AuthenticationManager` |
| `JwtAuthFilter` | `filter` | Turns a bearer token into an authenticated user; never throws on bad tokens |
| `JwtService` | `service` | Creates / validates tokens; secret and lifetime from configuration |
| `AdminUserDetails` | `config` | `UserDetails` wrapper around `AdminUser`; one authority named after the role |
| `AdminUserDetailsService` | `config` | `UserDetailsService` backed by `AdminUserRepository` |
| `AuthController` | `controller` | `POST /api/auth/login` (unchanged request/response), change-password, hash helper |

## Behaviour

* **Login** `POST /api/auth/login` → `{token, role, username}`. Wrong credentials → **401**.
* **Public URLs:** `/api/auth/**`, Swagger, `/actuator/health/**`, `/h2-console/**`. Everything else needs a valid token.
* **Missing / invalid / expired / tampered token → 401**, never 500. An invalid token on a public URL is ignored.
* **Roles come from the database, not from the token.** A deleted user is rejected immediately; a changed role applies to existing tokens immediately. The `role` claim inside the token is informational only, so a forged claim grants nothing.
* **Role rules** (only when `ENFORCE_ROLES=true`): members → ADMIN writes; books / issue / return → LIBRARIAN writes; reads → both. `/api/admin/**` → ADMIN always.
* Authorities are plain `ADMIN` / `LIBRARIAN`, so use `hasAuthority(...)` / `hasAnyAuthority(...)` (and `@PreAuthorize("hasAuthority('ADMIN')")`), **not** `hasRole`.

## Configuration

| Property / env var | Meaning |
|---|---|
| `jwt.secret` / `JWT_SECRET` | Signing key, **at least 32 characters** (the app refuses to start otherwise). Never commit a real one. |
| `jwt.expiration` / `JWT_EXPIRATION_MS` | Token lifetime in ms (default 86400000 = 24 h) |
| `app.security.enforce-roles` / `ENFORCE_ROLES` | API-level role enforcement |
| `app.security.password-hash-endpoint-enabled` | The public `/api/auth/getPassword` helper |
| `cors.allowed-origins` | Allowed browser origins |

## Tests added

`JwtServiceTest` (round trip, expiry, tampering, wrong secret, weak secret), `JwtAuthFilterTest` (no/odd header, valid, expired, garbage, deleted user, DB role wins over token claim), `SecurityIntegrationTest` (real filter chain: 401 cases, deleted user, role change, forged role claim).
