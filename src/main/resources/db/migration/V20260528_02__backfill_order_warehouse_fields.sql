UPDATE purchase_orders po
JOIN (
    SELECT
        sm.reference_id,
        MIN(sm.warehouse_id) AS warehouse_id
    FROM stock_movements sm
    WHERE sm.reference_type = 'PURCHASE_ORDER'
      AND sm.reference_id IS NOT NULL
      AND sm.warehouse_id IS NOT NULL
    GROUP BY sm.reference_id
    HAVING COUNT(DISTINCT sm.warehouse_id) = 1
) resolved ON resolved.reference_id = po.id
SET po.received_warehouse_id = resolved.warehouse_id
WHERE po.received_warehouse_id IS NULL
  AND po.status = 'RECEIVED';

UPDATE sales_orders so
JOIN (
    SELECT
        sm.reference_id,
        MIN(sm.warehouse_id) AS warehouse_id
    FROM stock_movements sm
    WHERE sm.reference_type = 'SALES_ORDER'
      AND sm.reference_id IS NOT NULL
      AND sm.warehouse_id IS NOT NULL
    GROUP BY sm.reference_id
    HAVING COUNT(DISTINCT sm.warehouse_id) = 1
) resolved ON resolved.reference_id = so.id
SET so.confirmed_warehouse_id = resolved.warehouse_id
WHERE so.confirmed_warehouse_id IS NULL
  AND so.status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED');
