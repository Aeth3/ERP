CREATE TABLE project_budget_lines (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    cost_code VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    budget_amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_project_budget_lines_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT uk_project_budget_lines_project_cost_code
        UNIQUE (project_id, cost_code)
);
