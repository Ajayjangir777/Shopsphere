-- "orders" (plural) because ORDER is a reserved word.
-- product_id is NOT a foreign key: products live in another service's database.
CREATE TABLE orders (
                        id                  UUID          PRIMARY KEY,
                        owner_sub           VARCHAR(100)  NOT NULL,
                        status              VARCHAR(20)   NOT NULL,
                        total_amount        NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    -- shipping address SNAPSHOT: a copy taken at order time, immune to later address edits
                        ship_recipient_name VARCHAR(100)  NOT NULL,
                        ship_line1          VARCHAR(150)  NOT NULL,
                        ship_line2          VARCHAR(150),
                        ship_city           VARCHAR(80)   NOT NULL,
                        ship_state          VARCHAR(80)   NOT NULL,
                        ship_postal_code    VARCHAR(12)   NOT NULL,
                        ship_country        VARCHAR(2)    NOT NULL,
                        ship_phone          VARCHAR(20),
                        version             BIGINT        NOT NULL DEFAULT 0,
                        created_at          TIMESTAMPTZ   NOT NULL,
                        updated_at          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_orders_owner_created ON orders (owner_sub, created_at DESC);

CREATE TABLE order_items (
                             id           UUID          PRIMARY KEY,
                             order_id     UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
                             product_id   UUID          NOT NULL,
                             product_name VARCHAR(150)  NOT NULL,   -- snapshot of the name at order time
                             unit_price   NUMERIC(12,2) NOT NULL CHECK (unit_price > 0),   -- snapshot of the price
                             quantity     INTEGER       NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);