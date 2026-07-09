CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(60)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    is_demo         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE listing (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id          BIGINT NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    city             VARCHAR(100) NOT NULL,
    address          VARCHAR(255) NOT NULL,
    price_per_night  DECIMAL(10,2) NOT NULL,
    max_guests       INT NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_host FOREIGN KEY (host_id) REFERENCES users(id)
);

CREATE INDEX idx_listing_city ON listing(city);
CREATE INDEX idx_listing_host ON listing(host_id);

CREATE TABLE listing_photo (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id  BIGINT NOT NULL,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_photo_listing FOREIGN KEY (listing_id) REFERENCES listing(id)
);

CREATE TABLE amenity (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE listing_amenity (
    listing_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (listing_id, amenity_id),
    CONSTRAINT fk_la_listing FOREIGN KEY (listing_id) REFERENCES listing(id),
    CONSTRAINT fk_la_amenity FOREIGN KEY (amenity_id) REFERENCES amenity(id)
);

CREATE TABLE booking (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id    BIGINT NOT NULL,
    guest_id      BIGINT NOT NULL,
    check_in      DATE NOT NULL,
    check_out     DATE NOT NULL,
    guest_count   INT NOT NULL,
    total_price   DECIMAL(10,2) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  TIMESTAMP NULL,
    CONSTRAINT fk_booking_listing FOREIGN KEY (listing_id) REFERENCES listing(id),
    CONSTRAINT fk_booking_guest FOREIGN KEY (guest_id) REFERENCES users(id)
);

CREATE INDEX idx_booking_listing_dates ON booking(listing_id, check_in, check_out);
CREATE INDEX idx_booking_guest ON booking(guest_id);

CREATE TABLE refresh_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_user ON refresh_token(user_id);
