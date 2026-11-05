-- General ledger schema: chart of accounts, account mappings, fiscal periods, journal entries and lines.
-- Idempotent so it is safe on databases whose tables Hibernate already created.
-- Seed accounts, mappings and the opening entry come from FinanceSetupService at startup.

CREATE TABLE IF NOT EXISTS gl_accounts (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS gl_account_mappings (
    mapping_key VARCHAR(64) PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES gl_accounts (id)
);

CREATE TABLE IF NOT EXISTS gl_fiscal_periods (
    id UUID PRIMARY KEY,
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    closed_by VARCHAR(255) NULL,
    closed_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT uq_gl_fiscal_periods_year_month UNIQUE (period_year, period_month)
);

CREATE TABLE IF NOT EXISTS gl_journal_entries (
    id UUID PRIMARY KEY,
    entry_number VARCHAR(32) NOT NULL UNIQUE,
    entry_date DATE NOT NULL,
    fiscal_period_id UUID NOT NULL REFERENCES gl_fiscal_periods (id),
    source_type VARCHAR(32) NOT NULL,
    source_id UUID NULL,
    stock_movement_id UUID NULL UNIQUE,
    memo VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL,
    reversal_of_id UUID NULL REFERENCES gl_journal_entries (id),
    reversed_by_id UUID NULL REFERENCES gl_journal_entries (id),
    total_amount DECIMAL(38, 2) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gl_journal_entries_date ON gl_journal_entries (entry_date);
CREATE INDEX IF NOT EXISTS idx_gl_journal_entries_source ON gl_journal_entries (source_type, source_id);

CREATE TABLE IF NOT EXISTS gl_journal_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES gl_journal_entries (id),
    line_number INTEGER NOT NULL,
    account_id UUID NOT NULL REFERENCES gl_accounts (id),
    debit DECIMAL(38, 2) NOT NULL DEFAULT 0,
    credit DECIMAL(38, 2) NOT NULL DEFAULT 0,
    project_id UUID NULL,
    warehouse_id UUID NULL,
    product_id UUID NULL,
    memo VARCHAR(255) NULL,
    CONSTRAINT ck_gl_journal_lines_one_sided CHECK (
        debit >= 0 AND credit >= 0 AND (debit = 0 OR credit = 0) AND (debit > 0 OR credit > 0))
);

CREATE INDEX IF NOT EXISTS idx_gl_journal_lines_entry ON gl_journal_lines (journal_entry_id);
CREATE INDEX IF NOT EXISTS idx_gl_journal_lines_account ON gl_journal_lines (account_id);
CREATE INDEX IF NOT EXISTS idx_gl_journal_lines_project ON gl_journal_lines (project_id);

-- Debits must equal credits for every entry, checked at commit time.
CREATE OR REPLACE FUNCTION gl_check_entry_balanced() RETURNS TRIGGER AS $$
DECLARE
    target_entry UUID;
    total_debit DECIMAL(38, 2);
    total_credit DECIMAL(38, 2);
BEGIN
    target_entry := COALESCE(NEW.journal_entry_id, OLD.journal_entry_id);
    SELECT COALESCE(SUM(debit), 0), COALESCE(SUM(credit), 0)
    INTO total_debit, total_credit
    FROM gl_journal_lines
    WHERE journal_entry_id = target_entry;

    IF total_debit <> total_credit THEN
        RAISE EXCEPTION 'Journal entry % is unbalanced: debit %, credit %',
            target_entry, total_debit, total_credit;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_gl_journal_lines_balanced ON gl_journal_lines;
CREATE CONSTRAINT TRIGGER trg_gl_journal_lines_balanced
    AFTER INSERT OR UPDATE OR DELETE ON gl_journal_lines
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION gl_check_entry_balanced();
