DROP PROCEDURE IF EXISTS gen_fact_inventory;
DELIMITER $$

CREATE PROCEDURE gen_fact_inventory()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_product_id BIGINT;
    DECLARE v_store_id BIGINT;
    DECLARE v_date_id DATE;
    DECLARE v_quantity INT;

    WHILE i <= 300 DO
        SET v_product_id = 6 + MOD(i - 1, 15);
        SET v_store_id = 1006 + MOD(i - 1, 9);
        SET v_date_id = DATE_SUB('2025-01-31', INTERVAL MOD(i - 1, 7) DAY);

        SET v_quantity =
            CASE v_product_id
                WHEN 6  THEN 35 + MOD(i, 12)
                WHEN 7  THEN 18 + MOD(i, 15)
                WHEN 8  THEN 12 + MOD(i, 17)
                WHEN 9  THEN 41 + MOD(i, 11)
                WHEN 10 THEN 22 + MOD(i, 14)
                WHEN 11 THEN 15 + MOD(i, 15)
                WHEN 12 THEN 32 + MOD(i, 12)
                WHEN 13 THEN 18 + MOD(i, 10)
                WHEN 14 THEN 12 + MOD(i, 9)
                WHEN 15 THEN 38 + MOD(i, 8)
                WHEN 16 THEN 16 + MOD(i, 11)
                WHEN 17 THEN 25 + MOD(i, 13)
                WHEN 18 THEN 33 + MOD(i, 10)
                WHEN 19 THEN 23 + MOD(i, 8)
                WHEN 20 THEN 5 + MOD(i, 6)
                ELSE 20
END;

INSERT INTO fact_inventory (
    inventory_id, product_id, store_id, date_id, quantity
) VALUES (
             i, v_product_id, v_store_id, v_date_id, v_quantity
         );

SET i = i + 1;
END WHILE;
END$$

DELIMITER ;

CALL gen_fact_inventory();
DROP PROCEDURE IF EXISTS gen_fact_inventory;
