-- ============================================
-- 维度表：商品维度 (dim_product)
-- ============================================
DROP TABLE IF EXISTS dim_product;
CREATE TABLE dim_product
(
    product_id    BIGINT PRIMARY KEY COMMENT '商品唯一ID',
    product_name  VARCHAR(255) NOT NULL COMMENT '商品名称',
    category_id   BIGINT NULL COMMENT '商品分类ID',
    category_name VARCHAR(255) NULL COMMENT '商品分类名称',
    brand         VARCHAR(255) NULL COMMENT '品牌',
    cost_price    DECIMAL(10, 2) NULL COMMENT '成本价',
    retail_price  DECIMAL(10, 2) NULL COMMENT '建议零售价'
) COMMENT='商品维度表';


-- ============================================
-- 维度表：门店维度 (dim_store)
-- ============================================
DROP TABLE IF EXISTS dim_store;
CREATE TABLE dim_store
(
    store_id   BIGINT PRIMARY KEY COMMENT '门店唯一ID',
    store_name VARCHAR(255) NOT NULL COMMENT '门店名称',
    province   VARCHAR(100) NULL COMMENT '所在省份',
    city       VARCHAR(100) NULL COMMENT '所在城市',
    address    VARCHAR(255) NULL COMMENT '门店详细地址',
    open_date  DATE NULL COMMENT '开业日期'
) COMMENT='门店维度表';


-- ============================================
-- 维度表：时间维度 (dim_date)
-- ============================================
DROP TABLE IF EXISTS dim_date;
CREATE TABLE dim_date
(
    date_id DATE PRIMARY KEY COMMENT '日期ID',
    year    INT NOT NULL,
    quarter INT NOT NULL,
    month   INT NOT NULL,
    day     INT NOT NULL,
    weekday INT NOT NULL
) COMMENT='时间维度表';


-- ============================================
-- 维度表：客户维度 (dim_customer)
-- ============================================
DROP TABLE IF EXISTS dim_customer;
CREATE TABLE dim_customer
(
    customer_id   BIGINT PRIMARY KEY COMMENT '客户ID',
    customer_name VARCHAR(255) NOT NULL,
    gender        VARCHAR(10) NULL,
    age           INT NULL,
    city          VARCHAR(100) NULL,
    province      VARCHAR(100) NULL
) COMMENT='客户维度表';


-- ============================================
-- 事实表：销售事实表 (fact_sales)
-- ============================================
DROP TABLE IF EXISTS fact_sales;
CREATE TABLE fact_sales
(
    sales_id     BIGINT PRIMARY KEY COMMENT '销售记录ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    store_id     BIGINT         NOT NULL COMMENT '门店ID',
    customer_id  BIGINT NULL COMMENT '客户ID',
    date_id      DATE           NOT NULL COMMENT '销售日期',
    quantity     INT            NOT NULL COMMENT '销售数量',
    sales_amount DECIMAL(10, 2) NOT NULL COMMENT '销售金额',
    discount     DECIMAL(10, 2) NULL COMMENT '折扣金额',
    FOREIGN KEY (product_id) REFERENCES dim_product (product_id),
    FOREIGN KEY (store_id) REFERENCES dim_store (store_id),
    FOREIGN KEY (customer_id) REFERENCES dim_customer (customer_id),
    FOREIGN KEY (date_id) REFERENCES dim_date (date_id)
) COMMENT='销售事实表';


-- ============================================
-- 事实表：库存事实表 (fact_inventory)
-- ============================================
DROP TABLE IF EXISTS fact_inventory;
CREATE TABLE fact_inventory
(
    inventory_id BIGINT PRIMARY KEY COMMENT '库存记录ID',
    product_id   BIGINT NOT NULL COMMENT '商品ID',
    store_id     BIGINT NOT NULL COMMENT '门店ID',
    date_id      DATE   NOT NULL COMMENT '库存日期',
    quantity     INT    NOT NULL COMMENT '库存数量',
    FOREIGN KEY (product_id) REFERENCES dim_product (product_id),
    FOREIGN KEY (store_id) REFERENCES dim_store (store_id),
    FOREIGN KEY (date_id) REFERENCES dim_date (date_id)
) COMMENT='库存事实表';
