# User Service QA

A user microservice for managing user data and calendar associations used as a backend for QA challenge.

## Quick Start

### Prerequisites

- Java 17+
- Docker and Docker Compose
- Maven

### Running the Application

1. **Start Dependencies**:
   ```bash
   docker-compose up -d
   ```
   This starts PostgreSQL, Kafka, Zookeeper, and Schema Registry.

2. **Build and Run**:
   ```bash
   mvn clean package
   java -jar target/svc-user-qa-0.0.1-SNAPSHOT.jar
   ```

   Or simply:
   ```bash
   mvn spring-boot:run
   ```

## Testing

### Run Tests
   ```bash
   mvn clean test
   ```

## Configuration

Key configuration properties (environment variables):

| Variable | Description | Default |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | Database URL | jdbc:postgresql://localhost:5432/userdb |
| `SPRING_DATASOURCE_USERNAME` | Database username | postgres |
| `SPRING_DATASOURCE_PASSWORD` | Database password | postgres |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | localhost:9093 |

## API Documentation

Once running, access the API documentation at:

```
# Swagger UI
http://localhost:8080/swagger-ui.html

# OpenAPI JSON
http://localhost:8080/api-docs
```

The API includes these main endpoints:
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `POST /api/users/{userId}/calendars/{calendarId}` - Add calendar to user
- `DELETE /api/users/{userId}/calendars/{calendarId}` - Remove calendar from user

## Accessing Admin Tools

### Database Access

You can connect to the PostgreSQL database using any PostgreSQL client:
```
Host: localhost
Port: 5432
Database: userdb
Username: postgres
Password: postgres
```

### Kafka UI

Monitor Kafka topics and messages via Kafka UI:
```
http://localhost:8090
```

### Schema Registry UI

View and manage Avro schemas:
```
http://localhost:8001
```

### Confluent Control Center (Optional)

If using Confluent Platform, access the Control Center at:
```
http://localhost:9021
```

