CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE user_calendars (
                                user_id UUID NOT NULL,
                                calendar_id UUID NOT NULL,
                                PRIMARY KEY (user_id, calendar_id),
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_email ON users(email);