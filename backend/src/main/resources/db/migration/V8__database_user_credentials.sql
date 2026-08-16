ALTER TABLE site_users
  ADD COLUMN login_name VARCHAR(80) NULL,
  ADD COLUMN password_hash VARCHAR(255) NULL,
  ADD UNIQUE INDEX uk_site_users_login_name (login_name);
