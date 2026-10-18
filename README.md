# LibraryOS

Library management system: members, catalogue, issue/return with automatic fees, reports, in-app notifications driven by Kafka events, and an optional local AI assistant (Ollama).

| Layer | Technology |
|---|---|
| Frontend | Angular 17 (standalone components), self-hosted fonts, nginx in Docker |
| Backend | Spring Boot 3.2, Java 21, Spring Security + JWT, Spring Data JPA, Flyway |
| Database | PostgreSQL 16 (H2 in tests) |
| Messaging | Apache Kafka (KRaft, spring-kafka) |
| Containers | Docker, Docker Compose |
| Tests | JUnit 5, Mockito, Spring Boot Test, spring-kafka-test (EmbeddedKafka), JaCoCo |

> **Verification status — read this first.** The sandbox this release was prepared in could not reach Maven Central and has no Docker daemon. The table says exactly what was and was not executed.
>
> | Item | Status |
> |---|---|
> | Angular production build (`ng build`) | ✅ Built successfully |
> | nginx config: syntax, SPA fallback, `/api` proxy, cache headers | ✅ Tested with real nginx + stub backend |
> | SQL migrations V1/V2 + seed on PostgreSQL 16 (fresh DB and existing DB, re-runs) | ✅ Executed with `psql` (not through Flyway itself) |
> | Docker Compose YAML structure | ✅ Parsed; ❌ never started |
> | Backend Java compile | ❌ **Not compiled** (only a JDK syntax-level parse, which was clean) |
> | 86 JUnit/Mockito/integration tests | ❌ **Written, never run** |
> | JaCoCo coverage | ❌ **Not measured — no number exists yet** |
> | Kafka producer/consumer against a live broker | ❌ Not run |
> | Running UI in a browser (visual/keyboard/screen-reader check) | ❌ Not done |
>
> Run `mvn clean verify` once (see Testing) — if anything fails to compile, that is the first thing to fix.

---

## Quick start (Docker)

```bash
cp .env.example .env          # then edit: set POSTGRES_PASSWORD, JWT_SECRET (>=32 chars), seed passwords
docker compose up --build
```

Open **http://localhost:4200** and sign in with the demo users created on first start (profile `docker`):
`admin` / `SEED_ADMIN_PASSWORD` and `librarian` / `SEED_LIBRARIAN_PASSWORD` (defaults `admin123` / `lib123` — change them in `.env`; use `SPRING_PROFILES_ACTIVE=prod` to skip demo users entirely).

| Service | Container name | Host port | Notes |
|---|---|---|---|
| frontend (nginx + Angular) | `frontend` | 4200 | proxies `/api/*` to `backend:8080` |
| backend (Spring Boot) | `backend` | 127.0.0.1:8080 | Swagger UI at `/swagger-ui.html`, health at `/actuator/health` |
| postgres | `postgres` | 127.0.0.1:5432 | volume `postgres-data` |
| kafka (KRaft) | `kafka` | 127.0.0.1:29092 | containers use `kafka:9092`; volume `kafka-data` |

Containers talk over the default Compose network by service name (`backend` → `postgres:5432`, `backend` → `kafka:9092`, nginx → `backend:8080`). The AI assistant needs Ollama on your machine (`OLLAMA_BASE_URL`, default `http://host.docker.internal:11434`); everything else works without it.

```bash
docker compose up -d --build     # start in background
docker compose ps                # status / health
docker compose logs -f backend   # follow logs (also: kafka, postgres, frontend)
docker compose stop              # stop, keep data
docker compose start             # start again
docker compose down              # remove containers, keep data volumes
docker compose down -v           # remove containers AND data (database + Kafka)
```

### Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | database | `lms`, `lms`, **required** |
| `JWT_SECRET` | token signing key, ≥ 32 chars | **required** |
| `JWT_EXPIRATION_MS` | token lifetime | 86400000 |
| `ENFORCE_ROLES` | API-level ADMIN/LIBRARIAN enforcement | `true` in Docker, `false` if unset |
| `PASSWORD_HASH_ENDPOINT_ENABLED` | the legacy public `/api/auth/getPassword` helper | `false` in Docker |
| `SPRING_PROFILES_ACTIVE` | `docker`/`dev` create demo users, `prod` does not | `docker` |
| `SEED_ADMIN_PASSWORD`, `SEED_LIBRARIAN_PASSWORD` | demo user passwords | `admin123`, `lib123` |
| `KAFKA_ENABLED`, `KAFKA_BOOTSTRAP_SERVERS` | Kafka switch / address | `true`, `kafka:9092` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `validate` |
| `FLYWAY_ENABLED` | run migrations on start | `true` |
| `CORS_ALLOWED_ORIGINS` | only if the UI is served from another origin | `http://localhost:4200` |
| `OLLAMA_BASE_URL`, `OLLAMA_MODEL` | AI assistant | see `.env.example` |
| `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT`, `KAFKA_EXTERNAL_PORT` | host ports | 4200, 8080, 5432, 29092 |

No secret is committed. The previous `application.properties` contained a Supabase host/username and a JWT secret — **rotate that JWT secret and the database password**, and treat the Supabase credentials as exposed if the repository was ever shared.

## Local setup (without Docker)

Needs Java 21, Maven, Node 20+/22, a PostgreSQL database, and optionally Kafka (set `KAFKA_ENABLED=false` to run without it — notifications then won't be generated).

```bash
# backend
export DB_URL=jdbc:postgresql://localhost:5432/lms DB_USERNAME=lms DB_PASSWORD=...
export JWT_SECRET=$(openssl rand -base64 48)
export KAFKA_ENABLED=false                 # or KAFKA_BOOTSTRAP_SERVERS=localhost:9092
cd backend && mvn spring-boot:run

# frontend (second terminal) – proxy.conf.json forwards /api to localhost:8080
cd frontend && npm install && npm start    # http://localhost:4200
```

## Architecture

```
Browser ── nginx (Angular, /api proxy) ── Spring Boot ── PostgreSQL
                                              │
                           IssueService (DB transaction commits)
                                              │  BookEvent (Spring event, AFTER_COMMIT)
                                       BookEventProducer ──► Kafka topic  book-events
                                                                   │
                                                  NotificationConsumer (group lms-notification-service)
                                                                   │
                                                 NotificationService ──► notifications table ──► bell in UI
```

Backend packages (`com.lms`): `controller`, `service`, `repository` (incl. JPA Specifications), `model` (entities), `dto`, `exception` (typed exceptions + `GlobalExceptionHandler`), `config` (security, JWT, seeder, `Clock`), `kafka`, `util` (`BillingCalculator`, `DueDates`, `PageUtils`). Existing code kept its layout; it was extended, not reorganised.

Frontend (`frontend/src/app`): `components/` (pages), `services/` (API, auth), `guards/` (route guards + interceptors), `models/`, `shared/` (modal, toast, confirm, pagination, empty state, searchable picker, notification bell, error-message helper), `styles.css` (one design system with tokens).

## Kafka

| | |
|---|---|
| Topic | `book-events` (3 partitions, 1 replica) and dead-letter `book-events.DLT` — created by `KafkaConfig` |
| Event | `BookEvent` JSON: `eventId`, `type` (`BOOK_ISSUED` / `BOOK_RETURNED`), `memberId`, `memberName`, `bookId`, `bookTitle`, `dueDate`, `billAmount`, `damaged`, `occurredAt`. Key = `memberId` (per-member ordering) |
| Producer | `BookEventProducer` — listens for the `BookEvent` that `IssueService` raises and sends it **after the DB commit**. Broker down ⇒ error logged, issue/return still succeeds |
| Consumer | `NotificationConsumer`, group `lms-notification-service` — parses and delegates to `NotificationService` |
| Idempotency | `notifications.event_id` is unique; a redelivered event creates no duplicate |
| Errors / retry | 3 retries, 1 s apart; unparseable messages are not retried; both end in `book-events.DLT` |
| Known limitation | If Kafka is down when a book is issued, that event is lost (no outbox). A transactional-outbox table is the next step if guaranteed delivery is needed |

Inspect: `docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic book-events --from-beginning`

## Database

Existing schema (unchanged): `admin_users`, `lib_members`, `books_info`, `issues`, `submissions`, `password_reset_tokens`.
Migrations live in `database/migrations` (packaged into the jar and run by Flyway):

| Script | What | Why |
|---|---|---|
| `V1__baseline_existing_schema.sql` | Your current DDL, `IF NOT EXISTS` | Creates tables on a fresh DB. On your **existing** DB Flyway baselines at version 1 (`baseline-on-migrate=true`) and skips it — nothing is altered |
| `V2__notifications_and_indexes.sql` | New table `notifications` (FKs to members/books with `ON DELETE SET NULL`, unique `event_id`); indexes on `issues(issue_status,due_date)`, `issues(mem_id/book_id/issue_dt)`, `submissions(mem_id/book_id/return_date)`, `books_info(book_genre, lower(book_title))`, `lib_members(lower(mem_name))` | Kafka consumer output; overdue/history/search performance. **No existing column or table is modified or dropped.** |
| `database/seed/demo_data.sql` | Optional demo books/members and one overdue loan (not run automatically) | Try the overdue report |
| `database/manual/rollback_V2.sql` | Manual undo of V2 | Safety |

Behaviour with your existing database: the first start creates `flyway_schema_history` and applies V2. Take a backup first. Hibernate now runs with `ddl-auto=validate` instead of `update`.
`due_date` is nullable in the old schema; rows without it are treated as *issue date + 10 days* (no data rewrite).
The `password_reset_tokens` table has no JPA entity in the code (the feature was never finished); it is untouched.

## API

**Existing endpoints — all preserved** (paths and response shapes unchanged; `IssueResponse` gained two additive fields):
`POST /api/auth/login`, `PUT /api/auth/change-password`, `POST /api/auth/getPassword`; `GET|POST /api/books`, `GET /api/books/{available|unavailable|search|{id}}`, `PUT|DELETE /api/books/{id}`; `GET|POST /api/members`, `GET /api/members/{id|search}`, `PUT|DELETE /api/members/{id}`; `POST /api/issue`, `POST /api/submit`, `GET /api/issues`, `/api/issues/member/{id}`, `/api/submissions`, `/api/submissions/member/{id}`, `/api/dashboard`; `POST /api/ai/{chat|recommend|classify-genre|overdue-message}`.

**New endpoints** (JWT required; readable by both roles):

| Endpoint | Purpose |
|---|---|
| `GET /api/books/page?query&genre&available&page&size&sort` | Paged, filtered, sorted catalogue (`sort=title,asc`; allowed: id,title,author,genre,price,quantity; size ≤ 100) |
| `GET /api/books/genres` | Distinct genres for filters |
| `GET /api/members/page?query&memberType&page&size&sort` | Paged member search (active members) |
| `GET /api/reports/overview` | Members, titles, loans, overdue, due-soon, 30-day issues/returns, fees, damaged |
| `GET /api/reports/overdue` | Overdue loans with days late and fee accrued so far |
| `GET /api/reports/due-soon?days=3` | Loans due within N days |
| `GET /api/reports/popular-books?limit=5` | Most-issued books |
| `GET /api/reports/activity?days=14` | Daily issues/returns (zero-filled) |
| `GET /api/reports/inventory` | Stock, copies on loan, out/low stock, value, by genre |
| `GET /api/reports/members/{id}` | Member activity: loans, returns, overdue, fees |
| `GET /api/notifications?unreadOnly&page&size`, `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, `POST /api/notifications/read-all` | Kafka-generated notifications |

Errors everywhere now use one JSON shape: `{timestamp,status,error,message,path,fieldErrors?}`. (The pre-existing plain-text 400 bodies from issue/return/member-delete are kept for compatibility.)

### Behaviour changes to existing endpoints (please review)

| Change | Reason |
|---|---|
| Wrong login → **401** with JSON (was 500) | Bug |
| Missing/expired token → **401** (was 403) | The Angular interceptor already expected 401 |
| Unknown book/member id → **404** (was 500) | Bug |
| Issuing: inactive member rejected; same member can't hold the same book twice | Duplicate active issues made later returns fail |
| Updating a book's quantity also updates `status` | A restocked book stayed "Unavailable" |
| Deleting a book with issue/return history → **409** with message (was 500) | FK violation |
| `@Valid`: negative price/quantity, missing issue/return IDs, over-long text → 400; new password ≥ 8 chars | Input validation |
| `IssueResponse`: new `overdue`, `daysOverdue`; legacy empty `dueDate` now computed | Needed by UI |
| `ENFORCE_ROLES` (default **off** unless set; **on** in Docker): members writes = ADMIN, catalogue/issue/return writes = LIBRARIAN, reads = both | Previously only the Angular guards separated roles |
| `PASSWORD_HASH_ENDPOINT_ENABLED=false` in Docker returns 404 for `/api/auth/getPassword` | It was a public hashing endpoint |
| `ddl-auto=validate`, JWT secret and DB settings only from environment | Safety |

## Frontend improvements

New design system and app shell (responsive drawer on tablet/mobile, skip link, landmarks, visible focus, reduced-motion support); server-side search/filter/sort/paging on Books and Members; searchable member/book pickers instead of typing numeric IDs; dashboard now shows real data (the old hard-coded "+12%" style trends were removed); overdue reports; member activity dialog; notification bell; accessible modal (focus trap, Escape, focus return), toasts and confirm dialog replacing `window.confirm`; table → card layout on phones; consistent loading, empty, error and success states; friendly error messages from the new error format; self-hosted fonts (the old Google Fonts import broke offline/Docker builds). The AI chat, genre auto-classification, post-issue recommendations and overdue-reminder drafting were kept. The frontend has no unit tests (the Angular project had no test target).

## Testing

```bash
cd backend
mvn clean verify          # or: mvn clean test
# no local Maven/Java? run it in a container:
docker run --rm -v "$PWD":/build -w /build maven:3.9-eclipse-temurin-21 mvn clean test
```
Needs access to Maven Central. Tests use H2 in memory (`application-test.properties`), no external services. 86 tests: `util` (billing, due dates, paging), `service` (issue/return rules and fees, books, members, reports, notifications — Mockito), `kafka` (producer, consumer), `integration` (full HTTP flow with real security on H2, role enforcement, and `KafkaNotificationFlowTest` using an embedded broker).

**Coverage (JaCoCo):** report is generated during `mvn test` at `backend/target/site/jacoco/index.html` (CSV/XML alongside). **Measured line/branch coverage: not yet measured** — fill it in here after your first run.

## Troubleshooting

* Backend won't start: `JWT_SECRET` missing (no default by design) or `DB_*` wrong — see `docker compose logs backend`.
* `Schema-validation` error at start on an old database: compare it with `database/migrations/V1…`; override once with `JPA_DDL_AUTO=update` if you must.
* No notifications: check `docker compose logs backend | grep -i kafka`; confirm the topic with the consumer command above.
* AI features show "Ollama" errors: start Ollama locally; nothing else depends on it.
