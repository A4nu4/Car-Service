# Car Rental API

Spring Boot 3.5 / Java 21 REST backend for the car rental frontend in the repository root.

## Quick start

**With Docker** (PostgreSQL and the API together):

```bash
cd backend
cp .env.example .env      # fill in DB_PASSWORD, JWT_SECRET, BOOTSTRAP_ADMIN_PASSWORD
docker compose up --build
```

**Without Docker** (needs PostgreSQL 13+ and JDK 21+; Maven is downloaded by the wrapper):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/car_rental DB_USERNAME=car_rental DB_PASSWORD=...
export JWT_SECRET=$(openssl rand -base64 48)
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com BOOTSTRAP_ADMIN_PASSWORD=...
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Once it is running:

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Interactive API documentation |
| http://localhost:8080/v3/api-docs | OpenAPI JSON (feed it to a client generator) |
| http://localhost:8080/actuator/health | Health check |

The default `dev` profile loads 3 demo branches and the 6 cars from the frontend mock data.
The `prod` profile loads no demo data and turns Swagger off (set `API_DOCS_ENABLED=true` to keep it).

## Tests

```bash
./mvnw test
```

- **Unit tests** (pricing, booking state machine, driver eligibility, booking service rules) always run.
- **Integration tests** start the full application against a real PostgreSQL. They use
  Testcontainers when Docker is available, or an existing database given as
  `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD` (its schema is wiped).
  Without either, they are skipped.

## For the frontend developer

Everything is in Swagger. The points below are what you need to know before you start.

**Authentication**
1. `POST /api/v1/auth/register` or `/login` returns `accessToken` (valid 15 min), `refreshToken` (valid 7 days) and `user`.
2. Send `Authorization: Bearer <accessToken>` on every protected call.
3. On a `401`, call `POST /api/v1/auth/refresh` with `{ "refreshToken": ... }` **once** and retry the original request.
   Each refresh token works only once. If two refreshes run in parallel with the same token, the second one
   counts as theft and logs the user out everywhere, so share a single in-flight refresh promise.
4. `POST /api/v1/auth/logout` with the refresh token.

**Conventions**
- Timestamps are ISO-8601 UTC instants: `2026-12-01T09:00:00Z`. Dates (birthday, licence expiry) are `YYYY-MM-DD`.
- Money is a JSON number with 2 decimals. The currency is in `price.currency` (EUR).
- Enum values are UPPER_CASE: `ECONOMY | SUV | ELECTRIC | VAN` and `AUTOMATIC | MANUAL`.
  The mock data in `src/types/car.ts` uses lowercase values, so map them in one place.
- Lists return `{ content, page, size, totalElements, totalPages }`. Query parameters: `?page=0&size=20&sort=pricePerDay,asc`.
- IDs are UUID strings.

**Errors** always look like this (`Content-Type: application/problem+json`):

```json
{
  "type": "https://api.car-rental.example/errors/validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-08T15:17:51Z",
  "errors": [{ "field": "password", "message": "must be 8-100 characters and contain at least one letter and one digit" }]
}
```

Branch on `code`, never on `detail`:

| Status | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Field errors in `errors[]` |
| 400 | `MALFORMED_REQUEST` | Bad JSON, a bad UUID, or an unknown sort field |
| 401 | `UNAUTHENTICATED` / `INVALID_CREDENTIALS` / `INVALID_REFRESH_TOKEN` | Missing or expired token, wrong login, bad refresh token |
| 403 | `FORBIDDEN` / `ACCOUNT_DISABLED` | Wrong role, or a disabled account |
| 404 | `NOT_FOUND` | Also returned for other customers' bookings |
| 409 | `CAR_UNAVAILABLE` | The car is already booked for that period |
| 409 | `INVALID_STATE_TRANSITION` | e.g. cancelling a completed booking |
| 409 | `EMAIL_ALREADY_REGISTERED` / `DUPLICATE_RESOURCE` / `CONCURRENT_MODIFICATION` | Conflicts with existing data |
| 422 | `DRIVER_NOT_ELIGIBLE` | Incomplete driver profile, under 21, or a licence that expires during the rental |
| 422 | `BUSINESS_RULE_VIOLATION` | e.g. cancelling inside the 24h window, a rental over 30 days |

**Typical customer flow**
1. `GET /api/v1/locations` fills the location dropdown.
2. `GET /api/v1/cars?locationId=…&pickupAt=…&dropoffAt=…&category=SUV` returns only cars that are free for that period.
3. `GET /api/v1/cars/{id}/quote?pickupAt=…&dropoffAt=…` returns the price and availability.
4. Register or log in, then `PUT /api/v1/account/driver-profile` (once).
5. `POST /api/v1/bookings` with `{ carId, pickupAt, dropoffAt, dropoffLocationId? }` returns `201` and the booking.
6. `GET /api/v1/bookings` lists "My bookings". `POST /bookings/{id}/cancel` and `/reschedule` work until `freeCancellationUntil`.

## Architecture

The code is organised by feature (`com.carrental.<module>`). Each module holds its controller, service,
repository, entities and DTOs.

| Module | Responsibility |
|---|---|
| `auth` | Register, login, JWT access tokens, rotating refresh tokens |
| `account` | The caller's own profile, password and driver details |
| `user` / `customer` | Staff administration, customer lookup, driver eligibility |
| `location` | Rental branches |
| `car` | Fleet management (admin) |
| `booking` | Public car search and availability, pricing, the booking lifecycle |
| `payment` | Payment and refund ledger per booking |
| `dashboard` | Statistics |
| `common`, `config`, `security` | Error handling, pagination, configuration, Spring Security |

Key decisions:
- **Double-booking is impossible.** Booking a car locks its row (`SELECT … FOR UPDATE`), and a PostgreSQL
  exclusion constraint (`ex_bookings_no_overlap`) rejects overlapping CONFIRMED or ACTIVE bookings even if the
  application code is bypassed. An integration test races six concurrent bookings, and exactly one wins.
- **Booking lifecycle:** `CONFIRMED → ACTIVE (pickup) → COMPLETED (return)`, plus `CANCELLED` and `NO_SHOW`.
  Transitions are methods on the `Booking` entity, so illegal moves are rejected in one place.
- **Prices are copied onto the booking.** Later price changes never alter existing bookings.
- **"Rented" is not a car status.** It is derived from ACTIVE bookings, so the two can never disagree.
  Car status (`AVAILABLE`, `MAINTENANCE`, `RETIRED`) is operational only.
- **Roles:** `CUSTOMER` (own bookings), `AGENT` (rental desk: bookings for customers, pickup, return,
  payments) and `ADMIN` (plus fleet, branches, staff and revenue statistics).
- **Secrets come only from environment variables.** The app refuses to start without `DB_PASSWORD` and `JWT_SECRET`.
  The first admin is created from `BOOTSTRAP_ADMIN_*` instead of a hardcoded migration.

Business rules (in `application.yml` under `app.rental`):

| Rule | Default |
|---|---|
| Minimum driver age | 21 |
| Maximum rental length | 30 days |
| Booking horizon | 365 days ahead |
| Free cancellation / reschedule | until 24h before pickup |
| Billing | per started 24h, minimum 1 day |
| Late return | grace of 59 min, then one daily rate per started late day |
| Late cancellation (staff) or no-show | one daily rate (staff can waive it) |
