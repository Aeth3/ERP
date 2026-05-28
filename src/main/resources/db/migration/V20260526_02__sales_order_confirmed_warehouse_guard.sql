ALTER TABLE sales_orders
    ADD COLUMN IF NOT EXISTS confirmed_warehouse_id BINARY(16) NULL AFTER customer_id;

CREATE INDEX IF NOT EXISTS idx_sales_orders_confirmed_warehouse_id
    ON sales_orders (confirmed_warehouse_id);
