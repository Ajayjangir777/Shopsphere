CREATE TABLE products (
                          id          UUID          PRIMARY KEY,
                          name        VARCHAR(150)  NOT NULL,
                          description VARCHAR(1000),
                          price       NUMERIC(12,2) NOT NULL CHECK (price > 0),
                          category    VARCHAR(100)  NOT NULL,
                          stock       INTEGER       NOT NULL CHECK (stock >= 0),
                          version     BIGINT        NOT NULL DEFAULT 0,
                          created_at  TIMESTAMPTZ   NOT NULL,
                          updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_products_category ON products (category);