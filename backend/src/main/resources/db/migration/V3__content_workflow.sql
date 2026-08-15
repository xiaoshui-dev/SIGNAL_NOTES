ALTER TABLE posts
  ADD COLUMN scheduled_at TIMESTAMP(6) NULL,
  ADD COLUMN deleted_at TIMESTAMP(6) NULL,
  ADD COLUMN seo_title VARCHAR(240) NULL,
  ADD COLUMN seo_description VARCHAR(320) NULL,
  ADD COLUMN canonical_url VARCHAR(500) NULL,
  ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE post_revisions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  title VARCHAR(240) NOT NULL,
  excerpt VARCHAR(1000) NOT NULL DEFAULT '',
  content LONGTEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  editor VARCHAR(80) NOT NULL DEFAULT 'system',
  change_summary VARCHAR(500) NOT NULL DEFAULT '保存文章',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_revision_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  CONSTRAINT uk_revision_post_version UNIQUE (post_id, version_no),
  INDEX idx_revisions_post_created (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  slug VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE share_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NULL,
  template VARCHAR(40) NOT NULL DEFAULT 'landscape',
  title_override VARCHAR(240),
  excerpt_override VARCHAR(1000),
  background_url VARCHAR(500),
  qr_label VARCHAR(120),
  version_no INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_share_config_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  INDEX idx_share_configs_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE share_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NULL,
  platform VARCHAR(40) NOT NULL,
  template VARCHAR(40) NOT NULL,
  device_type VARCHAR(20),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_share_event_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
  INDEX idx_share_events_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
