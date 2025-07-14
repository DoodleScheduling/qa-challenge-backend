-- Create user_calendars table with id as primary key
-- Note: No unique constraint on calendar_id to allow many-to-many relationship
CREATE TABLE user_calendars (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL,
    user_id UUID NOT NULL
);

-- Create a unique constraint on the combination of calendar_id and user_id
-- to ensure a user can't have the same calendar twice
ALTER TABLE user_calendars ADD CONSTRAINT user_calendars_calendar_id_user_id_key UNIQUE (calendar_id, user_id);

-- Create meetings table with calendar_id as foreign key to user_calendars
CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    location VARCHAR(255),
    calendar_id UUID NOT NULL,
    user_calendar_id UUID NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_calendar_id) REFERENCES user_calendars(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_user_calendars_user_id ON user_calendars(user_id);
CREATE INDEX idx_user_calendars_calendar_id ON user_calendars(calendar_id);
CREATE INDEX idx_meeting_calendar_id ON meetings(calendar_id);
CREATE INDEX idx_meeting_user_calendar_id ON meetings(user_calendar_id);
CREATE INDEX idx_meeting_time_range ON meetings(start_time, end_time);
