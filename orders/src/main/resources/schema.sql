
CREATE TABLE IF NOT EXISTS product (
    id          BIGSERIAL     NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    description TEXT         DEFAULT NULL,
    price       NUMERIC(12,2) NOT NULL,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS inventory (
    id                BIGSERIAL   NOT NULL,
    product_id        BIGINT      NOT NULL,
    quantity          INT         NOT NULL DEFAULT 0,
    reserved_quantity INT         NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT uk_inventory_product UNIQUE (product_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL     NOT NULL,
    username        VARCHAR(50)   NOT NULL,
    user_mobile     VARCHAR(50)   NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping_address TEXT         DEFAULT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_item (
    id          BIGSERIAL     NOT NULL,
    order_id    BIGINT        NOT NULL,
    product_id  BIGINT        NOT NULL,
    quantity    INT           NOT NULL DEFAULT 1,
    price       NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE IF NOT EXISTS cart (
    id         BIGSERIAL   NOT NULL,
    username   VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id         BIGSERIAL     NOT NULL,
    cart_id    BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL DEFAULT 1,
    added_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_item_cart    FOREIGN KEY (cart_id)    REFERENCES cart (id),
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT uk_cart_item_cart_product UNIQUE (cart_id, product_id)
);
