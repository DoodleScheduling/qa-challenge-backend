# PostgreSQL Database Setup

This directory contains the initialization scripts for PostgreSQL to support both microservices in the QA backend challenge.

## Database Configuration

The PostgreSQL instance is configured to create separate databases for each service:

- **svc_user_db**: Database for the user service (svc-user)
- **svc_calendar_db**: Database for the calendar service (svc-calendar)

## Initialization

The `init/01-create-databases.sql` script is automatically executed when the PostgreSQL container starts for the first time. It creates both databases and grants the necessary privileges to the `postgres` user.

## Connection Details

Both services are configured to connect to their respective databases:

### svc-user Service
- **Database**: `svc_user_db`
- **URL**: `jdbc:postgresql://localhost:5432/svc_user_db`
- **Username**: `postgres`
- **Password**: `postgres`

### svc-calendar Service
- **Database**: `svc_calendar_db`
- **URL**: `jdbc:postgresql://localhost:5432/svc_calendar_db`
- **Username**: `postgres`
- **Password**: `postgres`

## Usage

1. Start the services using Docker Compose:
   ```bash
   docker-compose up -d postgres
   ```

2. The databases will be automatically created and ready for use.

3. Each service will run its own Flyway migrations against its respective database when started.

## Testing

Both services use H2 in-memory databases for testing, so they don't depend on the PostgreSQL instance during test execution. 