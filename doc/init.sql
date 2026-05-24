-- ========================================
-- 库存/商品/仓库/销售/调拨管理数据库脚本
-- ========================================

-- 1. 商品表：记录商品的基本信息
DROP TABLE IF EXISTS bb_product;
CREATE TABLE bb_product
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_code VARCHAR(50)  NOT NULL UNIQUE COMMENT '商品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    spec         VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    unit         VARCHAR(20)  DEFAULT '件' COMMENT '计量单位',
    category     VARCHAR(50)  DEFAULT NULL COMMENT '分类',
    status       TINYINT      DEFAULT 1 COMMENT '状态（1：启用，0：停用）',
    created_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';


-- 2. 仓库表：存放仓库基本信息
DROP TABLE IF EXISTS bb_warehouse;
CREATE TABLE bb_warehouse
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warehouse_code VARCHAR(50)  NOT NULL UNIQUE COMMENT '仓库编码',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    location       VARCHAR(255) DEFAULT NULL COMMENT '仓库位置描述',
    manager        VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    status         TINYINT      DEFAULT 1 COMMENT '状态（1：启用，0：停用）',
    created_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库信息表';


-- 3. 库存表：记录每个仓库中每种商品的库存量
DROP TABLE IF EXISTS bb_inventory;
CREATE TABLE bb_inventory
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warehouse_id    BIGINT NOT NULL COMMENT '仓库ID',
    product_id      BIGINT NOT NULL COMMENT '商品ID',
    quantity        DECIMAL(18, 2) DEFAULT 0 COMMENT '当前库存数量',
    locked_quantity DECIMAL(18, 2) DEFAULT 0 COMMENT '锁定库存数量（例如调拨中未完成的部分）',
    updated_time    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_inventory_wh_prod (warehouse_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';


-- 4. 销售表
DROP TABLE IF EXISTS bb_sales_record;
CREATE TABLE bb_sales_record
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warehouse_id BIGINT         NOT NULL COMMENT '仓库ID',
    product_id   BIGINT         NOT NULL COMMENT '商品ID',
    sale_date    DATE           NOT NULL COMMENT '销售日期',
    quantity     DECIMAL(18, 2) NOT NULL COMMENT '销售数量',
    revenue      DECIMAL(18, 2) DEFAULT 0 COMMENT '销售金额',
    created_time DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品销售记录表';


-- 5. 调拨单主表
DROP TABLE IF EXISTS bb_transfer_order;
CREATE TABLE bb_transfer_order
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_no            VARCHAR(50) NOT NULL UNIQUE COMMENT '调拨单号',
    source_warehouse_id BIGINT      NOT NULL COMMENT '调出仓库ID',
    target_warehouse_id BIGINT      NOT NULL COMMENT '调入仓库ID',
    status              TINYINT     DEFAULT 0 COMMENT '状态（0：待审核，1：已审核，2：调拨中，3：已完成，4：已驳回）',
    transfer_type       INT         DEFAULT 0 COMMENT '调拨类型：1智能，0人工',
    transfer_date       DATE        DEFAULT NULL COMMENT '调拨日期',
    comment             TEXT COMMENT '说明',
    created_by          VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    approved_by         VARCHAR(50) DEFAULT NULL COMMENT '审核人',
    created_time        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单主表';


-- 6. 调拨单明细表
DROP TABLE IF EXISTS bb_transfer_order_item;
CREATE TABLE bb_transfer_order_item
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    transfer_order_id BIGINT         NOT NULL COMMENT '调拨单ID',
    product_id        BIGINT         NOT NULL COMMENT '商品ID',
    transfer_quantity DECIMAL(18, 2) NOT NULL COMMENT '调拨数量',
    actual_quantity   DECIMAL(18, 2) DEFAULT 0 COMMENT '实际到货数量',
    remark            VARCHAR(255)   DEFAULT NULL COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调拨单明细表';


-- 7. 库存操作日志表（用于追踪库存变动）
DROP TABLE IF EXISTS bb_inventory_log;
CREATE TABLE bb_inventory_log
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    warehouse_id    BIGINT         NOT NULL COMMENT '仓库ID',
    product_id      BIGINT         NOT NULL COMMENT '商品ID',
    change_type     VARCHAR(50)    NOT NULL COMMENT '变动类型（IN：入库，OUT：出库，TRANSFER_OUT：调出，TRANSFER_IN：调入）',
    quantity_change DECIMAL(18, 2) NOT NULL COMMENT '变动数量（正数增加，负数减少）',
    reference_no    VARCHAR(50)  DEFAULT NULL COMMENT '关联单据号（如调拨单号）',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存操作日志表';