CREATE TABLE customers
(
    id         varchar(36),
    first_name varchar(30),
    last_name  varchar(30),
    updated_ts timestamp
);

CREATE TABLE orders
(
    id         varchar(36),
    updated_ts timestamp
);