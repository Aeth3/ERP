ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS budget_amount DECIMAL(38, 2) NULL AFTER target_end_date;
