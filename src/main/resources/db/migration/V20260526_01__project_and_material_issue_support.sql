ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS project_id BINARY(16) NULL AFTER supplier_id;

ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS project_id BINARY(16) NULL AFTER warehouse_id;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_project_id
    ON purchase_orders (project_id);

CREATE INDEX IF NOT EXISTS idx_stock_movements_project_id
    ON stock_movements (project_id);

CREATE TABLE IF NOT EXISTS projects (
    id BINARY(16) NOT NULL,
    project_code VARCHAR(255) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    customer_id BINARY(16) NOT NULL,
    location VARCHAR(255) NULL,
    start_date DATE NULL,
    target_end_date DATE NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uk_projects_project_code UNIQUE (project_code)
);

CREATE INDEX IF NOT EXISTS idx_projects_customer_id
    ON projects (customer_id);

CREATE TABLE IF NOT EXISTS material_issues (
    id BINARY(16) NOT NULL,
    issue_number VARCHAR(255) NOT NULL,
    project_id BINARY(16) NOT NULL,
    warehouse_id BINARY(16) NOT NULL,
    remarks VARCHAR(255) NULL,
    performed_by VARCHAR(255) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_material_issues PRIMARY KEY (id),
    CONSTRAINT uk_material_issues_issue_number UNIQUE (issue_number)
);

CREATE INDEX IF NOT EXISTS idx_material_issues_project_id
    ON material_issues (project_id);

CREATE INDEX IF NOT EXISTS idx_material_issues_warehouse_id
    ON material_issues (warehouse_id);

CREATE TABLE IF NOT EXISTS material_issue_items (
    id BINARY(16) NOT NULL,
    material_issue_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    quantity DECIMAL(38, 2) NOT NULL,
    unit_cost DECIMAL(38, 2) NOT NULL,
    line_total DECIMAL(38, 2) NOT NULL,
    CONSTRAINT pk_material_issue_items PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_material_issue_items_issue_id
    ON material_issue_items (material_issue_id);

CREATE INDEX IF NOT EXISTS idx_material_issue_items_product_id
    ON material_issue_items (product_id);
