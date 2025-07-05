-- Add version column to users table for optimistic locking
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;