-- Create user_calendars table with id as primary key and calendar_id as a unique column
CREATE TABLE user_calendars (
    id UUID PRIMARY KEY,
    calendar_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL
);

-- Create meetings table with calendar_id as foreign key to user_calendars
CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    location VARCHAR(255),
    calendar_id UUID NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (calendar_id) REFERENCES user_calendars(calendar_id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_user_calendars_user_id ON user_calendars(user_id);
CREATE INDEX idx_user_calendars_calendar_id ON user_calendars(calendar_id);
CREATE INDEX idx_meeting_calendar_id ON meetings(calendar_id);
CREATE INDEX idx_meeting_time_range ON meetings(start_time, end_time);
