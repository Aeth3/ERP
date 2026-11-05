-- Accounts receivable: customer invoices (sales order, progress billing, retention
-- release), receipts, receipt allocations, and project contract fields.
-- Idempotent: Flyway does not currently run in this deployment (schema comes from
-- Hibernate ddl-auto=update); this script documents the schema and must be safe to
-- run against a database where Hibernate already created the tables.

CREATE TABLE IF NOT EXISTS ar_invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(32) NOT NULL UNIQUE,
    kind VARCHAR(32) NOT NULL,
    customer_id UUID NOT NULL,
    sales_order_id UUID,
    project_id UUID,
    invoice_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(16) NOT NULL,
    subtotal NUMERIC(38, 2) NOT NULL,
    retention_amount NUMERIC(38, 2) NOT NULL DEFAULT 0,
    total NUMERIC(38, 2) NOT NULL,
    amount_received NUMERIC(38, 2) NOT NULL DEFAULT 0,
    journal_entry_id UUID,
    notes VARCHAR(1024),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ar_invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES ar_invoices (id),
    line_number INTEGER NOT NULL,
    sales_order_item_id UUID,
    product_id UUID,
    description VARCHAR(255),
    quantity NUMERIC(38, 2),
    unit_price NUMERIC(38, 2),
    line_total NUMERIC(38, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS ar_receipts (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    receipt_date DATE NOT NULL,
    method VARCHAR(16) NOT NULL,
    reference VARCHAR(255),
    total NUMERIC(38, 2) NOT NULL,
    journal_entry_id UUID,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ar_receipt_allocations (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES ar_receipts (id),
    invoice_id UUID NOT NULL REFERENCES ar_invoices (id),
    amount NUMERIC(38, 2) NOT NULL
);

ALTER TABLE projects ADD COLUMN IF NOT EXISTS contract_amount NUMERIC(14, 2);
ALTER TABLE projects ADD COLUMN IF NOT EXISTS retention_rate NUMERIC(5, 2);

CREATE INDEX IF NOT EXISTS ix_ar_invoices_customer ON ar_invoices (customer_id);
CREATE INDEX IF NOT EXISTS ix_ar_invoices_sales_order ON ar_invoices (sales_order_id);
CREATE INDEX IF NOT EXISTS ix_ar_invoices_project ON ar_invoices (project_id);
CREATE INDEX IF NOT EXISTS ix_ar_invoice_lines_invoice ON ar_invoice_lines (invoice_id);
CREATE INDEX IF NOT EXISTS ix_ar_receipt_allocations_receipt ON ar_receipt_allocations (receipt_id);
CREATE INDEX IF NOT EXISTS ix_ar_receipt_allocations_invoice ON ar_receipt_allocations (invoice_id);
