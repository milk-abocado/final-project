CREATE DATABASE IF NOT EXISTS delivery;

-- 0) 안전 옵션 (테스트용)
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 스키마 생성 및 선택
CREATE SCHEMA IF NOT EXISTS delivery;

USE delivery;
-- images 테이블 (MySQL 8.x)
CREATE TABLE IF NOT EXISTS `images` (
                                        `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        `object_key`    VARCHAR(512) NOT NULL,     -- S3 object key
                                        `original_name` VARCHAR(255) NULL,
                                        `content_type`  VARCHAR(100) NULL,         -- e.g. image/png
                                        `size`          BIGINT UNSIGNED NULL,      -- bytes
                                        `ref_type`      VARCHAR(30) NOT NULL,      -- STORE / MENU / REVIEW
                                        `ref_id`        BIGINT NOT NULL,           -- 대상 엔티티 PK
                                        `purpose`       VARCHAR(30) NULL,          -- THUMB / GALLERY 등
                                        `owner_user_id` BIGINT NULL,               -- 업로더(옵션)
                                        `created_at`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        INDEX `idx_images_ref`       (`ref_type`, `ref_id`),
                                        INDEX `idx_images_owner`     (`owner_user_id`),
                                        INDEX `idx_images_created_at`(`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 2) 사용자(Users)
CREATE TABLE users (

    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    email               VARCHAR(100) UNIQUE            NOT NULL,
    password            VARCHAR(255)                   NOT NULL,
    name                VARCHAR(100),
    nickname            VARCHAR(100),
    phone_number        VARCHAR(15),
    address             VARCHAR(100),
    address_detail       VARCHAR(255),
    zip_code             VARCHAR(10),
    role                ENUM ('USER', 'OWNER','ADMIN') NOT NULL, -- USER / OWNER / ADMIN
    social_login        BOOLEAN                                 DEFAULT FALSE,
    created_at          TIMESTAMP                               DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP                               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    allow_notifications BOOLEAN                                 DEFAULT FALSE,
    is_deleted          BOOLEAN                                 DEFAULT FALSE,
    deleted             BOOLEAN                        NOT NULL DEFAULT FALSE

);

CREATE TABLE user_stars (
                            id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_id    BIGINT NOT NULL,
                            store_id   BIGINT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users (id),
                            FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE social_logins (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id     BIGINT       NOT NULL,
                               provider    VARCHAR(50)  NOT NULL, -- kakao, naver 등
                               provider_id VARCHAR(100) NOT NULL,
                               FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 2. 가게(Stores)
CREATE TABLE stores (
                        id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                        owner_id        BIGINT         NOT NULL,
                        name            VARCHAR(100)   NOT NULL,
                        address         VARCHAR(255)   NOT NULL,
    -- 위경도(주소 지오코딩 결과)
                        latitude        DECIMAL(10, 7) NOT NULL,
                        longitude       DECIMAL(10, 7) NOT NULL,
                        min_order_price INT            NOT NULL,
                        opens_at        TIME           NOT NULL,
                        closes_at       TIME           NOT NULL,
                        delivery_fee    INT            NOT NULL DEFAULT 0,    -- 배달비
    -- 폐업(논리 삭제) 전용 라이프사이클 상태
                        active          BOOLEAN        NOT NULL DEFAULT TRUE, -- 영업 OR 폐업
                        retired_at      TIMESTAMP               DEFAULT NULL, -- 폐업 처리 시각 기록
                        created_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
                        updated_at      TIMESTAMP               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- 검색/정렬 최적화를 위한 인덱스
CREATE INDEX idx_stores_active_name     ON stores (active, name);
CREATE INDEX idx_stores_active_lat_lng  ON stores (active, latitude, longitude);

CREATE TABLE store_notices (
                               id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                               store_id           BIGINT    NOT NULL,                                              -- 가게 ID
                               content            TEXT      NOT NULL,                                              -- 공지 내용
                               starts_at          TIMESTAMP NOT NULL,                                              -- 공지 시작 시각
                               ends_at            TIMESTAMP NOT NULL,                                              -- 공지 종료 시각
                               min_duration_hours INT       NOT NULL,                                              -- 공지 최소 유지 시간 (시간 단위)
                               max_duration_days  INT       NOT NULL,                                              -- 공지 최대 유지 시간 (일 단위)
                               created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                             -- 공지 생성 시각
                               updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 공지 수정 시각
                               FOREIGN KEY (store_id) REFERENCES stores (id)                                       -- 가게 ID와 연결
);

CREATE TABLE store_categories (
                                  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  store_id BIGINT      NOT NULL,
                                  category VARCHAR(50) NOT NULL,
                                  FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
);

ALTER TABLE store_categories
    MODIFY category VARCHAR(32) NOT NULL;

ALTER TABLE store_categories
    ADD CONSTRAINT uk_store_category UNIQUE (store_id, category);

CREATE INDEX idx_store_categories_category ON store_categories (category);

CREATE TABLE reviews (
                         id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                         store_id   BIGINT NOT NULL,
                         user_id    BIGINT NOT NULL,
                         order_id   BIGINT NOT NULL,
                         rating     INT,
                         content    TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         is_deleted BOOLEAN   DEFAULT FALSE,
                         FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
                         FOREIGN KEY (user_id) REFERENCES users (id),
                         FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE review_comments (
                                 id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 review_id  BIGINT        NOT NULL UNIQUE,
                                 owner_id   BIGINT        NOT NULL,
                                 store_id   BIGINT        NOT NULL,
                                 content    VARCHAR(1000) NOT NULL,
                                 created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP     NULL,
                                 is_deleted BOOLEAN       NOT NULL DEFAULT FALSE,
                                 deleted_at TIMESTAMP     NULL,
                                 CONSTRAINT fk_rr_review FOREIGN KEY (review_id) REFERENCES reviews (id),
                                 CONSTRAINT fk_rr_owner FOREIGN KEY (owner_id) REFERENCES users (id),
                                 CONSTRAINT fk_rr_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- 3. 메뉴(Menus)
CREATE TABLE menus (
                       id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                       store_id BIGINT                                 NOT NULL,
                       name     VARCHAR(100)                           NOT NULL,
                       price    INT                                    NOT NULL,
                       status   ENUM ('ACTIVE', 'DELETED', 'SOLD_OUT') NOT NULL,
                       FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
);


CREATE TABLE menu_categories (
                                 id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 menu_id  BIGINT      NOT NULL,
                                 category VARCHAR(50) NOT NULL,
                                 FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
);

CREATE TABLE menu_options (
                              id           BIGINT AUTO_INCREMENT PRIMARY KEY, -- 1
                              menu_id      BIGINT       NOT NULL,             -- 10
                              options_name VARCHAR(100) NOT NULL,             -- 온도 선택
                              min_select   INT,                               -- 1
                              max_select   INT,                               -- 1
                              is_required  BOOLEAN DEFAULT FALSE,
                              FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE
);

-- 그룹 안의 선택지 (HOT, ICE, Small, Large 등)
CREATE TABLE menu_option_choices (
                                     id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     group_id    BIGINT       NOT NULL,
                                     choice_name VARCHAR(100) NOT NULL, -- 예: HOT, ICE, Large
                                     extra_price INT DEFAULT 0,         -- 추가 요금
                                     FOREIGN KEY (group_id) REFERENCES menu_options (id) ON DELETE CASCADE
);


-- 4. 주문(Orders)
CREATE TABLE orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT                                                                                   NOT NULL,
    store_id    BIGINT                                                                                   NOT NULL,
    total_price INT                                                                                      NOT NULL,
    status      ENUM ('WAITING', 'ACCEPTED','COOKING' 'DELIVERING', 'COMPLETED', 'REJECTED', 'CANCELED') NOT NULL, -- 주문 상태
    -- REJECTED, CANCELED 추가 - 주문 거절(사장, 사용자), 주문 취소(고객센터)
    applied_coupon_id BIGINT,
    used_points INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (store_id) REFERENCES stores (id),
    FOREIGN KEY (applied_coupon_id) REFERENCES coupons(id)
);

CREATE TABLE order_items (
                             id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             menu_id  BIGINT NOT NULL,
                             quantity INT    NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders (id),
                             FOREIGN KEY (menu_id) REFERENCES menus (id)
);

CREATE TABLE order_options (
                               id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                               order_item_id     BIGINT       NOT NULL,
                               option_group_name VARCHAR(100) NOT NULL,
                               choice_name       VARCHAR(100) NOT NULL, -- 옵션명 (예: ICE, HOT, Large Size 등)
                               extra_price       INT          NOT NULL,
                               FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);

-- 5. 포인트/쿠폰
CREATE TABLE points (
                        id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id    BIGINT NOT NULL,
                        amount     INT    NOT NULL,
                        reason     VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE coupons (
                         id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                         code           VARCHAR(50) UNIQUE      NOT NULL,
                         type           ENUM ('RATE', 'AMOUNT') NOT NULL, -- RATE / AMOUNT
                         discount_value INT,
                         max_discount   INT,
                         created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         expire_at      TIMESTAMP
);

CREATE TABLE user_coupons (
                              id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id    BIGINT NOT NULL,
                              coupon_id  BIGINT NOT NULL,
                              is_used    BOOLEAN   DEFAULT FALSE,
                              used_at    TIMESTAMP NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (user_id) REFERENCES users (id),
                              FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

-- 6. 검색
CREATE TABLE searches (
                          id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                          keyword    VARCHAR(100) NOT NULL,
                          region     VARCHAR(50) NOT NULL,
                          count      INT NOT NULL DEFAULT 0,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          user_id    BIGINT NOT NULL,
                          FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 7. 알림
CREATE TABLE notifications (
                               id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id    BIGINT NOT NULL,
                               type       VARCHAR(50),
                               message    TEXT,
                               is_read    BOOLEAN,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 8. 주문 로그 (필요하다면 수정 혹은 삭제)
CREATE TABLE order_logs (
                            id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id   BIGINT,
                            store_id   BIGINT,
                            action     VARCHAR(50),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (order_id) REFERENCES orders (id),
                            FOREIGN KEY (store_id) REFERENCES stores (id)
);

-- 9. 이미지
-- 가게 / 메뉴 이미지
CREATE TABLE store_images (
                              id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                              ref_type   ENUM ('STORE', 'MENU') NOT NULL, -- 가게/메뉴 구분
                              ref_id     BIGINT                 NOT NULL,
                              url        VARCHAR(255),
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 리뷰 이미지
CREATE TABLE review_images (
                               id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                               review_id  BIGINT NOT NULL,
                               url        VARCHAR(255),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (review_id) REFERENCES reviews (id)
);

CREATE TABLE social_accounts (
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
                                 CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_social_user ON social_accounts (user_id);

ALTER TABLE users ADD COLUMN address VARCHAR(100);

ALTER TABLE reviews
    ADD COLUMN deleted_at DATETIME NULL,
    ADD COLUMN deleted_by VARCHAR(16) NULL,
    ADD COLUMN updated_at DATETIME NULL;

ALTER TABLE user_coupons
    ADD COLUMN used_at TIMESTAMP NULL;

CREATE INDEX idx_reviews_is_deleted ON reviews (is_deleted);

ALTER TABLE notifications MODIFY COLUMN user_id BIGINT NULL;
ALTER TABLE notifications ADD COLUMN status ENUM('SUCCESS','FAILED') NOT NULL;
ALTER TABLE notifications MODIFY COLUMN type ENUM('USER','ALL') NOT NULL;
ALTER TABLE notifications ADD COLUMN error_message VARCHAR(500);
