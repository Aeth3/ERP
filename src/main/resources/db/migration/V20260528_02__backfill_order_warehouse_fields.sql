UPDATE purchase_orders po
SET received_warehouse_id = resolved.warehouse_id
FROM (
    SELECT
        sm.reference_id,
        MIN(sm.warehouse_id) AS warehouse_id
    FROM stock_movements sm
    WHERE sm.reference_type = 'PURCHASE_ORDER'
      AND sm.reference_id IS NOT NULL
      AND sm.warehouse_id IS NOT NULL
    GROUP BY sm.reference_id
    HAVING COUNT(DISTINCT sm.warehouse_id) = 1
) resolved
WHERE po.received_warehouse_id IS NULL
  AND resolved.reference_id = po.id
  AND po.status = 'RECEIVED';

UPDATE sales_orders so
SET confirmed_warehouse_id = resolved.warehouse_id
FROM (
    SELECT
        sm.reference_id,
        MIN(sm.warehouse_id) AS warehouse_id
    FROM stock_movements sm
    WHERE sm.reference_type = 'SALES_ORDER'
      AND sm.reference_id IS NOT NULL
      AND sm.warehouse_id IS NOT NULL
    GROUP BY sm.reference_id
    HAVING COUNT(DISTINCT sm.warehouse_id) = 1
) resolved
WHERE so.confirmed_warehouse_id IS NULL
  AND resolved.reference_id = so.id
  AND so.status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED');
