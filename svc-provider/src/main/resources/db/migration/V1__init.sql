CREATE TABLE calendars (
                       id UUID PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description VARCHAR(1000)
);

CREATE TABLE events (
                       id UUID PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description VARCHAR(1000),
                       start_time TIMESTAMP NOT NULL,
                       end_time TIMESTAMP NOT NULL,
                       location VARCHAR(255),
                       calendar_id UUID NOT NULL,
                       FOREIGN KEY (calendar_id) REFERENCES calendars(id) ON DELETE CASCADE
);

CREATE INDEX idx_calendar_name ON calendars(name);
CREATE INDEX idx_event_calendar_id ON events(calendar_id);
CREATE INDEX idx_event_start_time ON events(start_time);