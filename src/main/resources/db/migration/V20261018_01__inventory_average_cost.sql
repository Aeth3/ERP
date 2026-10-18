ALTER TABLE inventory_stocks
    ADD COLUMN IF NOT EXISTS average_cost DECIMAL(38, 4) NOT NULL DEFAULT 0;

-- Seed existing stock with the weighted average of its purchase receipts.
UPDATE inventory_stocks s
SET average_cost = ROUND(p.average_cost, 4)
FROM (
    SELECT product_id,
           warehouse_id,
           SUM(quantity * unit_cost) / NULLIF(SUM(quantity), 0) AS average_cost
    FROM stock_movements
    WHERE movement_type = 'PURCHASE_IN'
      AND unit_cost IS NOT NULL
    GROUP BY product_id, warehouse_id
) p
WHERE s.product_id = p.product_id
  AND s.warehouse_id = p.warehouse_id
  AND p.average_cost IS NOT NULL;
