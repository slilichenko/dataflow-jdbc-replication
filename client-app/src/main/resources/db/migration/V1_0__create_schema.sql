/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE customers
(
    customer_id varchar(36)              NOT NULL PRIMARY KEY,
    first_name  varchar(30)              NOT NULL,
    last_name   varchar(30)              NOT NULL,
    updated_ts  timestamp with time zone NOT NULL
);
CREATE
INDEX customers_updated ON customers  (updated_ts);

CREATE TABLE orders
(
    order_id    varchar(36) PRIMARY KEY,
    customer_id varchar(36)              NOT NULL,
    status      varchar(12)              NOT NULL,
    updated_ts  timestamp with time zone NOT NULL,
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (customer_id)
);
CREATE
INDEX orders_updated ON orders  (updated_ts);

CREATE TABLE order_items
(
    order_item_id varchar(36) PRIMARY KEY,
    order_id      varchar(36)              NOT NULL,
    quantity      int                      NOT NULL,
    sku           varchar(12)              NOT NULL,
    updated_ts    timestamp with time zone NOT NULL,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
            REFERENCES orders (order_id)
);
CREATE
INDEX order_items_updated ON order_items  (updated_ts);

GRANT SELECT ON ALL TABLES IN SCHEMA public TO "data-reader";