ALTER TABLE contact_messages ADD COLUMN idempotency_key VARCHAR(100) NULL;
CREATE UNIQUE INDEX uk_contact_idempotency_key ON contact_messages (idempotency_key);
