CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  slug VARCHAR(100) NOT NULL UNIQUE,
  description VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug VARCHAR(180) NOT NULL UNIQUE,
  title VARCHAR(240) NOT NULL,
  excerpt VARCHAR(1000) NOT NULL DEFAULT '',
  content LONGTEXT NOT NULL,
  cover VARCHAR(500),
  cover_alt VARCHAR(300),
  category_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  author_name VARCHAR(80) NOT NULL DEFAULT '林默',
  published_at DATE,
  updated_at DATE NOT NULL,
  read_minutes INT NOT NULL DEFAULT 5,
  views BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id),
  INDEX idx_posts_status_published (status, published_at),
  INDEX idx_posts_category (category_id),
  FULLTEXT INDEX ft_posts_content (title, excerpt, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE post_tags (
  post_id BIGINT NOT NULL,
  tag VARCHAR(80) NOT NULL,
  PRIMARY KEY (post_id, tag),
  CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
  INDEX idx_post_tags_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_slug VARCHAR(180) NOT NULL,
  author_name VARCHAR(80) NOT NULL,
  email VARCHAR(180),
  content VARCHAR(2000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_comments_post_status (post_slug, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subscriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(180) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_settings (
  setting_key VARCHAR(100) PRIMARY KEY,
  setting_value TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE site_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  role VARCHAR(30) NOT NULL DEFAULT 'AUTHOR',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at TIMESTAMP(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE media_assets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  filename VARCHAR(255) NOT NULL,
  url VARCHAR(500) NOT NULL UNIQUE,
  mime_type VARCHAR(100) NOT NULL,
  size BIGINT NOT NULL,
  alt_text VARCHAR(300),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor VARCHAR(80) NOT NULL,
  action VARCHAR(100) NOT NULL,
  target VARCHAR(300) NOT NULL,
  result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
