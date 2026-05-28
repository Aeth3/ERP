ALTER TABLE purchase_orders
    ADD COLUMN received_warehouse_id UUID;

CREATE TABLE purchase_returns (
    id UUID PRIMARY KEY,
    return_number VARCHAR(255) NOT NULL UNIQUE,
    purchase_order_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    remarks VARCHAR(1000),
    performed_by VARCHAR(255) NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_purchase_returns_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id)
);

CREATE TABLE purchase_return_items (
    id UUID PRIMARY KEY,
    purchase_return_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity DECIMAL(19, 2) NOT NULL,
    unit_cost DECIMAL(19, 2) NOT NULL,
    line_total DECIMAL(19, 2) NOT NULL,
    CONSTRAINT fk_purchase_return_items_purchase_return
        FOREIGN KEY (purchase_return_id) REFERENCES purchase_returns (id)
);
