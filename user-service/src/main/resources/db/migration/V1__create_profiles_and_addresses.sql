-- owner_sub = the "sub" claim of the JWT (currently the username); NOT a foreign key,
-- because the user lives in the Auth Server's database, not this one.
CREATE TABLE user_profiles (
                               id         UUID         PRIMARY KEY,
                               owner_sub  VARCHAR(100) NOT NULL UNIQUE,
                               full_name  VARCHAR(100) NOT NULL,
                               phone      VARCHAR(20),
                               created_at TIMESTAMPTZ  NOT NULL,
                               updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE addresses (
                           id             UUID         PRIMARY KEY,
                           owner_sub      VARCHAR(100) NOT NULL,
                           label          VARCHAR(30)  NOT NULL,
                           recipient_name VARCHAR(100) NOT NULL,
                           line1          VARCHAR(150) NOT NULL,
                           line2          VARCHAR(150),
                           city           VARCHAR(80)  NOT NULL,
                           state          VARCHAR(80)  NOT NULL,
                           postal_code    VARCHAR(12)  NOT NULL,
                           country        VARCHAR(2)   NOT NULL,
                           phone          VARCHAR(20),
                           created_at     TIMESTAMPTZ  NOT NULL,
                           updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_addresses_owner ON addresses (owner_sub);