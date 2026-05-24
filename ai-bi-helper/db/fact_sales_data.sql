DROP PROCEDURE IF EXISTS gen_fact_sales;
DELIMITER $$

CREATE PROCEDURE gen_fact_sales()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_product_id BIGINT;
    DECLARE v_store_id BIGINT;
    DECLARE v_customer_id BIGINT;
    DECLARE v_date_id DATE;
    DECLARE v_quantity INT;
    DECLARE v_price DECIMAL(10,2);
    DECLARE v_discount DECIMAL(10,2);
    DECLARE v_sales_amount DECIMAL(10,2);

    WHILE i <= 300 DO
        SET v_product_id = 6 + MOD(i - 1, 15);
        SET v_store_id = 1006 + MOD(i - 1, 9);
        SET v_customer_id = 4 + MOD(i - 1, 29);
        SET v_date_id = DATE_ADD('2025-01-04', INTERVAL MOD(i - 1, 28) DAY);
        SET v_quantity = 1 + MOD(i - 1, 4);

SELECT retail_price INTO v_price
FROM dim_product
WHERE product_id = v_product_id;

SET v_sales_amount = v_price * v_quantity;

        SET v_discount =
            CASE MOD(i - 1, 7)
                WHEN 0 THEN 0.00
                WHEN 1 THEN ROUND(v_sales_amount * 0.04, 2)
                WHEN 2 THEN ROUND(v_sales_amount * 0.03, 2)
                WHEN 3 THEN ROUND(v_sales_amount * 0.05, 2)
                WHEN 4 THEN ROUND(v_sales_amount * 0.025, 2)
                WHEN 5 THEN ROUND(v_sales_amount * 0.00, 2)
                ELSE ROUND(v_sales_amount * 0.02, 2)
END;

INSERT INTO fact_sales (
    sales_id, product_id, store_id, customer_id, date_id, quantity, sales_amount, discount
) VALUES (
             i, v_product_id, v_store_id, v_customer_id, v_date_id, v_quantity, v_sales_amount, v_discount
         );

SET i = i + 1;
END WHILE;
END$$

DELIMITER ;

CALL gen_fact_sales();
DROP PROCEDURE IF EXISTS gen_fact_sales;


