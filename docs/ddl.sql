-- 카페 예약 시스템 DDL

CREATE DATABASE IF NOT EXISTS cafe_reservation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE cafe_reservation;

-- 사용자
CREATE TABLE users (
    user_id       VARCHAR(50)   NOT NULL,
    email         VARCHAR(100)  NOT NULL,
    password      VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    refresh_token VARCHAR(512)  NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email)
);

-- 메뉴
CREATE TABLE menu (
    menu_id     BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    price       INT           NOT NULL,
    category    VARCHAR(30)   NOT NULL,
    image_path  VARCHAR(255)  NULL,
    description VARCHAR(500)  NULL,
    is_available BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (menu_id)
);

-- 주문
CREATE TABLE orders (
    order_id     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      VARCHAR(50)  NOT NULL,
    order_type   VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    total_amount INT          NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 주문 항목
CREATE TABLE order_item (
    order_item_id BIGINT  NOT NULL AUTO_INCREMENT,
    order_id      BIGINT  NOT NULL,
    menu_id       BIGINT  NOT NULL,
    quantity      INT     NOT NULL,
    price         INT     NOT NULL,
    PRIMARY KEY (order_item_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT fk_order_item_menu  FOREIGN KEY (menu_id)  REFERENCES menu (menu_id)
);

-- 좌석
CREATE TABLE seat (
    seat_id     BIGINT      NOT NULL AUTO_INCREMENT,
    seat_number VARCHAR(10) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    seat_row    VARCHAR(5)  NOT NULL,
    seat_col    INT         NOT NULL,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (seat_id),
    UNIQUE KEY uk_seat_number (seat_number)
);

-- 예약
CREATE TABLE reservation (
    reservation_id BIGINT   NOT NULL AUTO_INCREMENT,
    order_id       BIGINT   NOT NULL,
    seat_id        BIGINT   NOT NULL,
    start_time     DATETIME NOT NULL,
    end_time       DATETIME NOT NULL,
    status         VARCHAR(20) NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reservation_id),
    CONSTRAINT fk_reservation_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT fk_reservation_seat  FOREIGN KEY (seat_id)  REFERENCES seat (seat_id)
);

-- 결제
CREATE TABLE payment (
    payment_id   BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL,
    imp_uid      VARCHAR(100) NULL,
    merchant_uid VARCHAR(50)  NOT NULL,
    amount       INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    pay_method   VARCHAR(30)  NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payment_order    (order_id),
    UNIQUE KEY uk_payment_imp      (imp_uid),
    UNIQUE KEY uk_payment_merchant (merchant_uid),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
);
