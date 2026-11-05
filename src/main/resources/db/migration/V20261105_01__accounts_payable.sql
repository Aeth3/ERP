-- Accounts payable: supplier bills, bill lines, payments, payment allocations.
-- Idempotent: Flyway does not currently run in this deployment (schema comes from
-- Hibernate ddl-auto=update); this script documents the schema and must be safe to
-- run against a database where Hibernate already created the tables.

CREATE TABLE IF NOT EXISTS ap_bills (
    id UUID PRIMARY KEY,
    bill_number VARCHAR(64) NOT NULL,
    supplier_id UUID NOT NULL,
    purchase_order_id UUID,
    bill_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(16) NOT NULL,
    subtotal NUMERIC(38, 2) NOT NULL,
    total NUMERIC(38, 2) NOT NULL,
    amount_paid NUMERIC(38, 2) NOT NULL DEFAULT 0,
    journal_entry_id UUID,
    notes VARCHAR(1024),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ap_bill_lines (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL REFERENCES ap_bills (id),
    line_number INTEGER NOT NULL,
    product_id UUID,
    purchase_order_item_id UUID,
    quantity NUMERIC(38, 2),
    unit_cost NUMERIC(38, 2),
    account_id UUID,
    description VARCHAR(255),
    line_total NUMERIC(38, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS ap_payments (
    id UUID PRIMARY KEY,
    payment_number VARCHAR(32) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(16) NOT NULL,
    reference VARCHAR(255),
    total NUMERIC(38, 2) NOT NULL,
    journal_entry_id UUID,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ap_payment_allocations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES ap_payments (id),
    bill_id UUID NOT NULL REFERENCES ap_bills (id),
    amount NUMERIC(38, 2) NOT NULL
);

-- A supplier cannot have two live bills with the same invoice number;
-- cancelled bills do not block reuse. (Also enforced in ApBillService.)
CREATE UNIQUE INDEX IF NOT EXISTS ux_ap_bills_supplier_bill_number
    ON ap_bills (supplier_id, bill_number)
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS ix_ap_bills_supplier ON ap_bills (supplier_id);
CREATE INDEX IF NOT EXISTS ix_ap_bills_purchase_order ON ap_bills (purchase_order_id);
CREATE INDEX IF NOT EXISTS ix_ap_bill_lines_bill ON ap_bill_lines (bill_id);
CREATE INDEX IF NOT EXISTS ix_ap_payment_allocations_payment ON ap_payment_allocations (payment_id);
CREATE INDEX IF NOT EXISTS ix_ap_payment_allocations_bill ON ap_payment_allocations (bill_id);
