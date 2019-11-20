CREATE TABLE product
(
    reference    SERIAL PRIMARY KEY,
    name         text,
    price        float,
    supply_price float
);
ALTER SEQUENCE product_reference_seq RESTART WITH 4;
ALTER TABLE product REPLICA IDENTITY FULL;

CREATE TABLE product_order
(
    id SERIAL PRIMARY KEY
);
ALTER SEQUENCE product_order_id_seq RESTART WITH 101;
ALTER TABLE product_order REPLICA IDENTITY FULL;

CREATE TABLE order_line
(
    id        SERIAL PRIMARY KEY,
    order_id  SERIAL REFERENCES product_order (id),
    reference SERIAL REFERENCES product (reference),
    quantity  int NOT NULL default 1
);
ALTER SEQUENCE order_line_id_seq RESTART WITH 1001;
ALTER TABLE order_line REPLICA IDENTITY FULL;


CREATE TABLE product_margin
(
    product_reference INTEGER PRIMARY KEY,
    product_name      TEXT,
    total_margin      FLOAT CHECK (total_margin >= 0)
);
ALTER TABLE product_margin REPLICA IDENTITY FULL;

CREATE TABLE consumed_event
(
    hash TEXT PRIMARY KEY
);