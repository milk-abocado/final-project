CREATE DATABASE IF NOT EXISTS delivery;
USE delivery;

SET FOREIGN_KEY_CHECKS = 0;

-- 0. 공통 이미지
CREATE TABLE images
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    object_key    VARCHAR(512) NOT NULL, -- S3 object key
    original_name VARCHAR(255),
    content_type  VARCHAR(100),          -- e.g. image/png
    size          BIGINT UNSIGNED,       -- bytes
    ref_type      VARCHAR(30)  NOT NULL, -- STORE / MENU / REVIEW
    ref_id        BIGINT       NOT NULL, -- 대상 엔티티 PK
    purpose       VARCHAR(30),           -- THUMB / GALLERY 등
    owner_user_id BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_images_ref (ref_type, ref_id),
    INDEX idx_images_owner (owner_user_id),
    INDEX idx_images_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 1. 사용자
CREATE TABLE users
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(100) UNIQUE           NOT NULL,
    password            VARCHAR(255)                  NOT NULL,
    name                VARCHAR(100),
    nickname            VARCHAR(100),
    phone_number        VARCHAR(15),
    address             VARCHAR(100),
    address_detail      VARCHAR(255),
    zip_code            VARCHAR(10),
    role                ENUM ('USER','OWNER','ADMIN') NOT NULL,
    social_login        BOOLEAN                                DEFAULT FALSE,
    allow_notifications BOOLEAN                                DEFAULT FALSE,
    is_deleted          BOOLEAN                                DEFAULT FALSE,
    deleted             BOOLEAN                       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP                              DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP                              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE social_logins
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    provider    VARCHAR(50)  NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE social_accounts
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    provider          VARCHAR(20)  NOT NULL,
    provider_user_id  VARCHAR(100) NOT NULL,
    provider_id       VARCHAR(100) NOT NULL,
    email             VARCHAR(255),
    display_name      VARCHAR(100),
    profile_image_url VARCHAR(500),
    refresh_token     VARCHAR(512),
    connected_at      DATETIME(6),
    created_at        DATETIME(6),
    updated_at        DATETIME(6),
    CONSTRAINT uk_social_provider_subject UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_social_user (user_id)
);

CREATE TABLE user_stars
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    store_id   BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- 2. 가게
CREATE TABLE stores
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT         NOT NULL,
    name            VARCHAR(100)   NOT NULL,
    address         VARCHAR(255)   NOT NULL,
    latitude        DECIMAL(10, 7) NOT NULL,
    longitude       DECIMAL(10, 7) NOT NULL,
    min_order_price INT            NOT NULL,
    opens_at        TIME           NOT NULL,
    closes_at       TIME           NOT NULL,
    delivery_fee    INT            NOT NULL DEFAULT 0,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    retired_at      TIMESTAMP      NULL,
    created_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users (id),
    INDEX idx_stores_active_name (active, name),
    INDEX idx_stores_active_lat_lng (active, latitude, longitude)
);

CREATE TABLE store_notices
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id           BIGINT    NOT NULL,
    content            TEXT      NOT NULL,
    starts_at          TIMESTAMP NOT NULL,
    ends_at            TIMESTAMP NOT NULL,
    min_duration_hours INT       NOT NULL,
    max_duration_days  INT       NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE store_categories
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT      NOT NULL,
    category VARCHAR(32) NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    UNIQUE KEY uk_store_category (store_id, category),
    INDEX idx_store_categories_category (category)
);

-- 3. 메뉴
CREATE TABLE menus
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT                               NOT NULL,
    name     VARCHAR(100)                         NOT NULL,
    price    INT                                  NOT NULL,
    status   ENUM ('ACTIVE','DELETED','SOLD_OUT') NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
);

CREATE TABLE menu_categories
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id  BIGINT      NOT NULL,
    category VARCHAR(50) NOT NULL,
    FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
);

CREATE TABLE menu_options
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id      BIGINT       NOT NULL,
    options_name VARCHAR(100) NOT NULL,
    min_select   INT,
    max_select   INT,
    is_required  BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
);

CREATE TABLE menu_option_choices
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id    BIGINT       NOT NULL,
    choice_name VARCHAR(100) NOT NULL,
    extra_price INT DEFAULT 0,
    FOREIGN KEY (group_id) REFERENCES menu_options (id) ON DELETE CASCADE
);

-- 4. 주문
CREATE TABLE orders
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT                                                                               NOT NULL,
    store_id          BIGINT                                                                               NOT NULL,
    total_price       INT                                                                                  NOT NULL,
    status            ENUM ('WAITING','ACCEPTED','COOKING','DELIVERING','COMPLETED','REJECTED','CANCELED') NOT NULL,
    applied_coupon_id BIGINT,
    used_points       INT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (store_id) REFERENCES stores (id),
    FOREIGN KEY (applied_coupon_id) REFERENCES coupons (id)
);

CREATE TABLE order_items
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_id  BIGINT NOT NULL,
    quantity INT    NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders (id),
    FOREIGN KEY (menu_id) REFERENCES menus (id)
);

CREATE TABLE order_options
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id     BIGINT       NOT NULL,
    option_group_name VARCHAR(100) NOT NULL,
    choice_name       VARCHAR(100) NOT NULL,
    extra_price       INT          NOT NULL,
    FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);

CREATE TABLE order_logs
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT,
    store_id   BIGINT,
    action     VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders (id),
    FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- 5. 리뷰
CREATE TABLE reviews
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    order_id   BIGINT      NOT NULL,
    rating     INT,
    content    TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN   DEFAULT FALSE,
    deleted_at DATETIME    NULL,
    deleted_by VARCHAR(16) NULL,
    updated_at DATETIME    NULL,
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (order_id) REFERENCES orders (id),
    INDEX idx_reviews_is_deleted (is_deleted)
);

CREATE TABLE review_comments
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id  BIGINT        NOT NULL UNIQUE,
    owner_id   BIGINT        NOT NULL,
    store_id   BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NULL,
    is_deleted BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP     NULL,
    FOREIGN KEY (review_id) REFERENCES reviews (id),
    FOREIGN KEY (owner_id) REFERENCES users (id),
    FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE review_images
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id  BIGINT NOT NULL,
    url        VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews (id)
);

-- 6. 포인트/쿠폰
CREATE TABLE points
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    amount     INT    NOT NULL,
    reason     VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE coupons
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(50) UNIQUE     NOT NULL,
    type           ENUM ('RATE','AMOUNT') NOT NULL,
    discount_value INT,
    max_discount   INT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_at      TIMESTAMP
);

CREATE TABLE user_coupons
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    coupon_id  BIGINT    NOT NULL,
    is_used    BOOLEAN   DEFAULT FALSE,
    used_at    TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

-- 7. 검색
CREATE TABLE searches
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword    VARCHAR(100) NOT NULL,
    region     VARCHAR(50)  NOT NULL,
    count      INT          NOT NULL DEFAULT 0,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    user_id    BIGINT       NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE popular_searches
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword    VARCHAR(100) NOT NULL,
    region     VARCHAR(50),
    search_count      INT          NOT NULL DEFAULT 0,
    ranking       INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. 알림
CREATE TABLE notifications
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT                    NULL,
    type          ENUM ('USER','ALL')       NOT NULL,
    message       TEXT,
    is_read       BOOLEAN,
    status        ENUM ('SUCCESS','FAILED') NOT NULL,
    error_message VARCHAR(500),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

SET FOREIGN_KEY_CHECKS = 1;