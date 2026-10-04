CREATE TABLE users (
                       id            UUID         PRIMARY KEY,
                       username      VARCHAR(50)  NOT NULL UNIQUE,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(100) NOT NULL,   -- "{bcrypt}" prefix + 60-char hash
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL,
                       updated_at    TIMESTAMPTZ  NOT NULL
);

-- One row per role a user has (ADMIN, CUSTOMER, ...), without the ROLE_ prefix
CREATE TABLE user_roles (
                            user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                            role    VARCHAR(30) NOT NULL,
                            PRIMARY KEY (user_id, role)
);