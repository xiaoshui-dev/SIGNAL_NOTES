ALTER TABLE comments
  ADD COLUMN parent_id BIGINT NULL AFTER id,
  ADD COLUMN report_count INT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN report_reason VARCHAR(240) NULL AFTER report_count,
  ADD CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
  ADD INDEX idx_comments_parent (parent_id);
