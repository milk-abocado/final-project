CREATE DATABASE IF NOT EXISTS delivery;
USE delivery;

-- 1. 사용자(Users)

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    nickname VARCHAR(100),
    phone_number VARCHAR(15),
    role ENUM('USER', 'OWNER','ADMIN') NOT NULL, -- USER / OWNER / ADMIN
    social_login BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    allow_notifications BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- 2. 가게(Stores) - 수정 예정
CREATE TABLE stores (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        owner_id BIGINT,
                        name VARCHAR(100),
                        address VARCHAR(255),
                        min_order_price INT,
                        opens_at TIME,
                        closes_at TIME,
                        delivery_fee BIGINT DEFAULT 0, -- 배달비
    -- 폐업(논리 삭제) 전용 라이프사이클 상태
                        active BOOLEAN DEFAULT TRUE, -- 영업 OR 폐업
                        retired_at TIMESTAMP DEFAULT NULL,                   -- 폐업 처리 시각 기록
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE user_stars (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE social_logins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL, -- kakao, naver 등
    provider_id VARCHAR(100) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE store_notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,              -- 가게 ID
    content TEXT NOT NULL,                 -- 공지 내용
    starts_at TIMESTAMP NOT NULL,          -- 공지 시작 시각
    ends_at TIMESTAMP NOT NULL,            -- 공지 종료 시각
    min_duration_hours INT NOT NULL,       -- 공지 최소 유지 시간 (시간 단위)
    max_duration_days INT NOT NULL,        -- 공지 최대 유지 시간 (일 단위)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 공지 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 공지 수정 시각
    FOREIGN KEY (store_id) REFERENCES stores(id)   -- 가게 ID와 연결
);

CREATE TABLE store_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- 4. 주문(Orders)
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        store_id BIGINT NOT NULL,
                        total_price INT NOT NULL,
                        status ENUM('WAITING', 'ACCEPTED', 'DELIVERING', 'COMPLETED', 'REJECTED', 'CANCELED') NOT NULL, -- 주문 상태
    -- REJECTED, CANCELED 추가 - 주문 거절(사장, 사용자), 주문 취소(고객센터)
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (store_id) REFERENCES stores(id)
);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    rating INT,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE review_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- 3. 메뉴(Menus)
CREATE TABLE menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    status ENUM('ACTIVE', 'DELETED', 'SOLD_OUT') NOT NULL,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);


CREATE TABLE menu_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE
);

CREATE TABLE menu_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, -- 1
    menu_id BIGINT NOT NULL, -- 10
    options_name VARCHAR(100) NOT NULL, -- 온도 선택
    min_select INT, -- 1
    max_select INT, -- 1
    is_required BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (menu_id) REFERENCES menus(id)ON DELETE CASCADE
);

-- 그룹 안의 선택지 (HOT, ICE, Small, Large 등)
CREATE TABLE menu_option_choices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    choice_name VARCHAR(100) NOT NULL,  -- 예: HOT, ICE, Large
    extra_price INT DEFAULT 0,          -- 추가 요금
    FOREIGN KEY (group_id) REFERENCES menu_options(id) ON DELETE CASCADE
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (menu_id) REFERENCES menus(id)
);

CREATE TABLE order_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    option_group_name VARCHAR(100) NOT NULL,
    choice_name VARCHAR(100) NOT NULL, -- 옵션명 (예: ICE, HOT, Large Size 등)
    extra_price INT NOT NULL,
    FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);

-- 5. 포인트/쿠폰
CREATE TABLE points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    type ENUM('RATE', 'AMOUNT') NOT NULL, -- RATE / AMOUNT
    discount_value INT,
    max_discount INT,
    expire_at TIMESTAMP
);

CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

-- 6. 검색 (피드백 후 수정)
CREATE TABLE searches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100),
    region VARCHAR(50),
    count INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 7. 알림
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50),
    message TEXT,
    is_read BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 8. 주문 로그 (필요하다면 수정 혹은 삭제)
CREATE TABLE order_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    store_id BIGINT,
    action VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (store_id) REFERENCES stores(id)
);

-- 9. 이미지
-- 가게 / 메뉴 이미지
CREATE TABLE store_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ref_type ENUM('STORE', 'MENU') NOT NULL, -- 가게/메뉴 구분
    ref_id BIGINT NOT NULL,
    url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 리뷰 이미지
CREATE TABLE review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES reviews(id)
);
