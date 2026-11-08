-- btree_gist lets an exclusion constraint combine "same car" (=) with "overlapping time range" (&&).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------------------
-- Users and authentication
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(30),
    role           VARCHAR(20)  NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'AGENT', 'CUSTOMER'))
);

CREATE INDEX ix_users_role ON users (role);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------------
-- Customer driver profiles (1:1 with users of role CUSTOMER)
-- ---------------------------------------------------------------------------
CREATE TABLE customer_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    date_of_birth           DATE,
    driver_license_number   VARCHAR(50),
    driver_license_country  VARCHAR(2),
    driver_license_expiry   DATE,
    address_line            VARCHAR(255),
    city                    VARCHAR(100),
    postal_code             VARCHAR(20),
    country                 VARCHAR(2),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_customer_profiles_user UNIQUE (user_id)
);

-- ---------------------------------------------------------------------------
-- Rental branches
-- ---------------------------------------------------------------------------
CREATE TABLE locations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(120) NOT NULL,
    address_line   VARCHAR(255) NOT NULL,
    city           VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    country        VARCHAR(2)      NOT NULL,
    phone          VARCHAR(30),
    opening_hours  VARCHAR(255),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_locations_name UNIQUE (name)
);

CREATE INDEX ix_locations_city ON locations (lower(city));

-- ---------------------------------------------------------------------------
-- Fleet
-- ---------------------------------------------------------------------------
CREATE TABLE cars (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand                VARCHAR(60)   NOT NULL,
    model                VARCHAR(60)   NOT NULL,
    model_year           INTEGER       NOT NULL,
    category             VARCHAR(20)   NOT NULL,
    transmission         VARCHAR(20)   NOT NULL,
    fuel_type            VARCHAR(20)   NOT NULL,
    seats                INTEGER       NOT NULL,
    price_per_day        NUMERIC(10, 2) NOT NULL,
    image_url            VARCHAR(500),
    vin                  VARCHAR(17)      NOT NULL,
    registration_number  VARCHAR(20)   NOT NULL,
    mileage              INTEGER       NOT NULL DEFAULT 0,
    status               VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
    location_id          UUID          NOT NULL REFERENCES locations (id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_cars_vin UNIQUE (vin),
    CONSTRAINT uq_cars_registration UNIQUE (registration_number),
    CONSTRAINT ck_cars_category CHECK (category IN ('ECONOMY', 'SUV', 'ELECTRIC', 'VAN')),
    CONSTRAINT ck_cars_transmission CHECK (transmission IN ('AUTOMATIC', 'MANUAL')),
    CONSTRAINT ck_cars_fuel_type CHECK (fuel_type IN ('PETROL', 'DIESEL', 'HYBRID', 'ELECTRIC')),
    CONSTRAINT ck_cars_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'RETIRED')),
    CONSTRAINT ck_cars_seats CHECK (seats BETWEEN 1 AND 9),
    CONSTRAINT ck_cars_price CHECK (price_per_day > 0),
    CONSTRAINT ck_cars_mileage CHECK (mileage >= 0),
    CONSTRAINT ck_cars_model_year CHECK (model_year BETWEEN 1990 AND 2100)
);

CREATE INDEX ix_cars_location_status ON cars (location_id, status);
CREATE INDEX ix_cars_category ON cars (category);

-- ---------------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------------
CREATE TABLE bookings (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference            VARCHAR(20)    NOT NULL,
    customer_id          UUID           NOT NULL REFERENCES users (id),
    car_id               UUID           NOT NULL REFERENCES cars (id),
    pickup_location_id   UUID           NOT NULL REFERENCES locations (id),
    dropoff_location_id  UUID           NOT NULL REFERENCES locations (id),
    pickup_at            TIMESTAMPTZ    NOT NULL,
    dropoff_at           TIMESTAMPTZ    NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    daily_rate           NUMERIC(10, 2) NOT NULL,
    rental_days          INTEGER        NOT NULL,
    base_price           NUMERIC(12, 2) NOT NULL,
    late_fee             NUMERIC(12, 2) NOT NULL DEFAULT 0,
    extra_charges        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cancellation_fee     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_price          NUMERIC(12, 2) NOT NULL,
    amount_paid          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    customer_notes       VARCHAR(1000),
    actual_pickup_at     TIMESTAMPTZ,
    actual_return_at     TIMESTAMPTZ,
    pickup_mileage       INTEGER,
    return_mileage       INTEGER,
    return_notes         VARCHAR(1000),
    cancelled_at         TIMESTAMPTZ,
    cancellation_reason  VARCHAR(500),
    created_by           UUID           NOT NULL REFERENCES users (id),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_bookings_reference UNIQUE (reference),
    CONSTRAINT ck_bookings_status CHECK (status IN ('CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_bookings_period CHECK (dropoff_at > pickup_at),
    CONSTRAINT ck_bookings_rental_days CHECK (rental_days >= 1),
    CONSTRAINT ck_bookings_amounts CHECK (
        base_price >= 0 AND late_fee >= 0 AND extra_charges >= 0
        AND cancellation_fee >= 0 AND total_price >= 0 AND amount_paid >= 0
    ),
    CONSTRAINT ck_bookings_return_mileage CHECK (return_mileage IS NULL OR return_mileage >= pickup_mileage),
    -- The database itself guarantees a car is never double-booked, even under concurrent requests.
    CONSTRAINT ex_bookings_no_overlap EXCLUDE USING gist (
        car_id WITH =,
        tstzrange(pickup_at, dropoff_at, '[)') WITH &&
    ) WHERE (status IN ('CONFIRMED', 'ACTIVE'))
);

CREATE INDEX ix_bookings_customer ON bookings (customer_id, pickup_at DESC);
CREATE INDEX ix_bookings_car ON bookings (car_id, pickup_at);
CREATE INDEX ix_bookings_status_pickup ON bookings (status, pickup_at);
CREATE INDEX ix_bookings_status_dropoff ON bookings (status, dropoff_at);
CREATE INDEX ix_bookings_pickup_location ON bookings (pickup_location_id, pickup_at);

-- ---------------------------------------------------------------------------
-- Payments (append-only ledger per booking)
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id   UUID           NOT NULL REFERENCES bookings (id),
    type         VARCHAR(10)    NOT NULL,
    method       VARCHAR(20)    NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL,
    reference    VARCHAR(100),
    paid_at      TIMESTAMPTZ    NOT NULL,
    recorded_by  UUID           NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_payments_type CHECK (type IN ('CHARGE', 'REFUND')),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH', 'CARD', 'BANK_TRANSFER')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);

CREATE INDEX ix_payments_booking ON payments (booking_id);
CREATE INDEX ix_payments_paid_at ON payments (paid_at);
