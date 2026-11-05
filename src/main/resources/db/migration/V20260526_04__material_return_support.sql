CREATE TABLE IF NOT EXISTS material_returns (
    id UUID NOT NULL,
    return_number VARCHAR(255) NOT NULL,
    material_issue_id UUID NOT NULL,
    project_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    remarks VARCHAR(255) NULL,
    performed_by VARCHAR(255) NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_material_returns PRIMARY KEY (id),
    CONSTRAINT uk_material_returns_return_number UNIQUE (return_number)
);

CREATE INDEX IF NOT EXISTS idx_material_returns_material_issue_id
    ON material_returns (material_issue_id);

CREATE INDEX IF NOT EXISTS idx_material_returns_project_id
    ON material_returns (project_id);

CREATE INDEX IF NOT EXISTS idx_material_returns_warehouse_id
    ON material_returns (warehouse_id);

CREATE TABLE IF NOT EXISTS material_return_items (
    id UUID NOT NULL,
    material_return_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity DECIMAL(38, 2) NOT NULL,
    unit_cost DECIMAL(38, 2) NOT NULL,
    line_total DECIMAL(38, 2) NOT NULL,
    CONSTRAINT pk_material_return_items PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_material_return_items_return_id
    ON material_return_items (material_return_id);

CREATE INDEX IF NOT EXISTS idx_material_return_items_product_id
    ON material_return_items (product_id);
