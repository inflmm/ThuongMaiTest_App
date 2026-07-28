CREATE TABLE product_specs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT,
    spec_key VARCHAR(255),   -- Ví dụ: 'CPU', 'Dung lượng', 'Tần số quét'
    spec_value VARCHAR(255), -- Ví dụ: 'i5-14400F', '16GB', '144Hz'
    FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE TABLE product_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT,
    image_url VARCHAR(255),
    is_thumbnail BIT DEFAULT 0, -- Xác định ảnh nào làm đại diện
    sort_order INT,             -- Thứ tự hiển thị trong slider
    FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE TABLE promotions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    discount_percent INT,    -- Giảm %
    discount_amount DECIMAL, -- Hoặc giảm thẳng số tiền
    start_date DATETIME,
    end_date DATETIME
);

-- Bảng nối để biết sản phẩm nào đang được áp dụng khuyến mãi nào
CREATE TABLE product_promotions (
    product_id BIGINT,
    promotion_id BIGINT,
    PRIMARY KEY (product_id, promotion_id)
);

-- Tạo 1 Collection mới
INSERT INTO collections (name, slug) VALUES ('PC Gaming Cao Cấp', 'pc-gaming-high-end');

-- Gán sản phẩm ID 9 (PC MSI Project Zero trong file SQL của bạn) vào Collection này
INSERT INTO collection_product (collection_id, product_id) VALUES (1, 9);

-- 1. Thêm sản phẩm trước
INSERT INTO product (name, slug, price, long_description, deleted) 
VALUES ('Laptop Acer Nitro 5', 'laptop-acer-nitro-5', 20000000, 'Nội dung mô tả cực dài...', b'0');

-- 2. Thêm Album ảnh (Giả sử Product vừa tạo có ID là 1)
-- Bạn gán sort_order để biết ảnh nào hiện trước trong slider
INSERT INTO product_image (product_id, image_url, sort_order) VALUES (1, '/images/acer-1.jpg', 1);
INSERT INTO product_image (product_id, image_url, sort_order) VALUES (1, '/images/acer-2.jpg', 2);

-- 3. Thêm Thông số kỹ thuật
INSERT INTO product_specification (product_id, spec_key, spec_value) VALUES (1, 'CPU', 'Intel Core i5-12500H');
INSERT INTO product_specification (product_id, spec_key, spec_value) VALUES (1, 'RAM', '8GB DDR4');


-- 1. Tạo Collection
INSERT INTO collections (name, slug) VALUES ('Sản phẩm bán chạy', 'best-seller');

-- 2. Gán sản phẩm ID 1 vào Collection ID 1
-- Lưu ý: Tên bảng 'collection_product' phải khớp với @JoinTable bạn đặt ở Entity Collection
INSERT INTO collection_product (collection_id, product_id) VALUES (1, 1);