ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS confirmed_warehouse_id UUID;

CREATE INDEX IF NOT EXISTS idx_sales_orders_confirmed_warehouse_id
    ON sales_orders (confirmed_warehouse_id);
