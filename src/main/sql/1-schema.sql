CREATE TABLE product
(
    reference    SERIAL PRIMARY KEY,
    name         text,
    price        float,
    supply_price float
);
ALTER SEQUENCE product_reference_seq RESTART WITH 4;


CREATE TABLE product_order
(
    id SERIAL PRIMARY KEY
);
ALTER SEQUENCE product_order_id_seq RESTART WITH 101;

CREATE TABLE order_line
(
    id        SERIAL PRIMARY KEY,
    order_id  SERIAL REFERENCES product_order (id),
    reference SERIAL REFERENCES product (reference),
    quantity  int NOT NULL default 1
);
ALTER SEQUENCE order_line_id_seq RESTART WITH 1001;

CREATE TABLE product_margin
(
    product_reference INTEGER PRIMARY KEY,
    product_name      TEXT,
    total_margin      FLOAT CHECK (total_margin >= 0)
);

CREATE TABLE order_event
(
    event_id            SERIAL PRIMARY KEY,
    event_type          text NOT NULL,
    product_order       text NOT NULL
);