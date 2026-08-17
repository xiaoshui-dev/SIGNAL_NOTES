ALTER TABLE site_users
  ADD COLUMN avatar_url VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL AFTER name;

ALTER TABLE posts
  ADD COLUMN author_id BIGINT NULL AFTER author_name,
  ADD INDEX idx_posts_author (author_id),
  ADD CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES site_users(id) ON DELETE SET NULL;

UPDATE posts
SET author_id = (
      SELECT MIN(candidate.id)
      FROM site_users candidate
      WHERE candidate.role = 'ADMIN' AND candidate.status = 'ACTIVE'
    ),
    author_name = (
      SELECT selected.name
      FROM site_users selected
      WHERE selected.id = (
        SELECT MIN(candidate.id)
        FROM site_users candidate
        WHERE candidate.role = 'ADMIN' AND candidate.status = 'ACTIVE'
      )
    )
WHERE author_name = '林默'
  AND author_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM site_users candidate
    WHERE candidate.role = 'ADMIN' AND candidate.status = 'ACTIVE'
  );
