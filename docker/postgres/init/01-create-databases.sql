-- Create database for svc-user service
CREATE DATABASE svc_user_db;

-- Create database for svc-calendar service  
CREATE DATABASE svc_calendar_db;

-- Grant all privileges to postgres user for both databases
GRANT ALL PRIVILEGES ON DATABASE svc_user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE svc_calendar_db TO postgres; 