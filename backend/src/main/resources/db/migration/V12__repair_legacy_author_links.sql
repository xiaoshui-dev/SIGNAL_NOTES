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
WHERE author_name IN ('林默', '站点作者')
  AND author_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM site_users candidate
    WHERE candidate.role = 'ADMIN' AND candidate.status = 'ACTIVE'
  );
