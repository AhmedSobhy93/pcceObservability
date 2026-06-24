ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS owner VARCHAR(160);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS team VARCHAR(160);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS milestone VARCHAR(200);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS depends_on VARCHAR(700);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS blocked_by VARCHAR(700);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS risk VARCHAR(40);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS deliverable VARCHAR(700);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS share_with VARCHAR(500);
ALTER TABLE project_tasks ADD COLUMN IF NOT EXISTS external_ref VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_project_tasks_owner ON project_tasks(owner);
CREATE INDEX IF NOT EXISTS idx_project_tasks_team ON project_tasks(team);
CREATE INDEX IF NOT EXISTS idx_project_tasks_milestone ON project_tasks(milestone);
