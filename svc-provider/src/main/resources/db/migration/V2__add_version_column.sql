-- Add version column to calendars table for optimistic locking
ALTER TABLE calendars ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Add version column to events table for optimistic locking
ALTER TABLE events ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;