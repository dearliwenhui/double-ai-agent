
-- ========================================
-- 1. 创建仓库样例数据
-- ========================================
INSERT INTO bb_warehouse (warehouse_code, warehouse_name, location, manager)
VALUES
    ('WH001', '华北仓', '北京', '张三'),
    ('WH002', '华东仓', '上海', '李四'),
    ('WH003', '华南仓', '广州', '王五');


-- ========================================
-- 2. 创建商品样例数据
-- ========================================
INSERT INTO bb_product (product_code, product_name, spec, unit, category)
VALUES
    ('P001', '智能手机', '6.5英寸 128G', '台', '电子产品'),
    ('P002', '蓝牙耳机', '入耳式', '副', '电子产品'),
    ('P003', '办公椅', '可调节靠背', '张', '家具'),
    ('P004', '显示器', '27寸 IPS', '台', '电子产品'),
    ('P005', '机械键盘', '104键 RGB', '个', '外设');


-- ========================================
-- 3. 初始化库存（每仓库每商品）
-- ========================================
INSERT INTO bb_inventory (warehouse_id, product_id, quantity, locked_quantity)
SELECT
    w.id,
    p.id,
    FLOOR(RAND() * 200 + 100),
    0
FROM bb_warehouse w, bb_product p;


-- ========================================
-- 4. 创建过去三年的季度销售数据（2023-2025）
-- 每季度每仓库每商品一条
-- ========================================
SET @start_date = '2023-01-01';
SET @end_date   = '2025-12-31';

INSERT INTO bb_sales_record (warehouse_id, product_id, sale_date, quantity, revenue)
SELECT *
FROM (
         SELECT
             w.id AS warehouse_id,
             p.id AS product_id,
             DATE_ADD('2023-01-01', INTERVAL (FLOOR(RAND() * 12 * 3)) MONTH) AS sale_date,
             ROUND(RAND() * 500 + 50, 2) AS quantity,
             ROUND(RAND() * 50000 + 5000, 2) AS revenue
         FROM bb_warehouse w,
              bb_product p,
              (
                  SELECT 1 FROM dual
                  UNION SELECT 2
                  UNION SELECT 3
                  UNION SELECT 4
              ) q
     ) t
WHERE t.sale_date BETWEEN @start_date AND @end_date;


-- ========================================
-- 5. 创建调拨单主表（模拟每季度一次调拨）
-- 插入随机调拨单数据（排除相同仓库）
-- ========================================
INSERT INTO bb_transfer_order (
    order_no,
    source_warehouse_id,
    target_warehouse_id,
    status,
    created_by,
    transfer_type,
    transfer_date
)
SELECT
    t.order_no,
    t.source_warehouse_id,
    t.target_warehouse_id,
    t.status,
    t.created_by,
    t.transfer_type,
    t.transfer_date
FROM (
         SELECT
             CONCAT('TO', LPAD(FLOOR(RAND() * 99999), 5, '0')) AS order_no,
             FLOOR(1 + RAND() * 3) AS source_warehouse_id,
             FLOOR(1 + RAND() * 3) AS target_warehouse_id,
             3 AS status,                  -- 已完成
             '系统自动' AS created_by,
             1 AS transfer_type,           -- 假设 1=系统自动
             DATE_ADD('2023-01-01', INTERVAL FLOOR(RAND() * 36) MONTH) AS transfer_date
         FROM (
                  SELECT 1 FROM dual
                  UNION SELECT 2
                  UNION SELECT 3
                  UNION SELECT 4
                  UNION SELECT 5
              ) tmp
     ) t
WHERE t.source_warehouse_id <> t.target_warehouse_id;


-- ========================================
-- 6. 创建调拨明细数据
-- ========================================
INSERT INTO bb_transfer_order_item (
    transfer_order_id,
    product_id,
    transfer_quantity,
    actual_quantity,
    remark
)
SELECT
    o.id,
    p.id,
    ROUND(RAND() * 50 + 10, 2),
    ROUND(RAND() * 50 + 10, 2),
    '季度调拨'
FROM bb_transfer_order o, bb_product p
WHERE o.status = 3
ORDER BY o.id
    LIMIT 50;


-- ========================================
-- 7. 为库存操作日志生成随机调拨记录 - 调出
-- ========================================
INSERT INTO bb_inventory_log (
    warehouse_id,
    product_id,
    change_type,
    quantity_change,
    reference_no,
    remark
)
SELECT
    t.source_warehouse_id,
    item.product_id,
    'TRANSFER_OUT',
    -item.transfer_quantity,
    o.order_no,
    '系统模拟调拨出库'
FROM bb_transfer_order o
         JOIN bb_transfer_order_item item ON o.id = item.transfer_order_id
         JOIN bb_transfer_order t ON o.id = t.id;


-- ========================================
-- 8. 为库存操作日志生成随机调拨记录 - 调入
-- ========================================
INSERT INTO bb_inventory_log (
    warehouse_id,
    product_id,
    change_type,
    quantity_change,
    reference_no,
    remark
)
SELECT
    t.target_warehouse_id,
    item.product_id,
    'TRANSFER_IN',
    item.transfer_quantity,
    o.order_no,
    '系统模拟调拨入库'
FROM bb_transfer_order o
         JOIN bb_transfer_order_item item ON o.id = item.transfer_order_id
         JOIN bb_transfer_order t ON o.id = t.id;
