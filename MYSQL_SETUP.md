# MySQL Database Setup Instructions

## Current Configuration
The system is currently running with H2 in-memory database for local development since MySQL is not installed on this system.

## Switching to MySQL

### 1. Install MySQL
- Download and install MySQL Community Server from https://dev.mysql.com/downloads/mysql/
- During installation, set a root password
- Start the MySQL service

### 2. Create Database
```sql
CREATE DATABASE peps_production;
```

### 3. Configure Environment Variables
Set the following environment variables before starting the backend:

```bash
# Windows (Command Prompt)
set DB_URL=jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=UTC
set DB_USERNAME=root
set DB_PASSWORD=your_password
set DB_DRIVER=com.mysql.cj.jdbc.Driver
set DB_DIALECT=org.hibernate.dialect.MySQLDialect

# Windows (PowerShell)
$env:DB_URL="jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:DB_DRIVER="com.mysql.cj.jdbc.Driver"
$env:DB_DIALECT="org.hibernate.dialect.MySQLDialect"
```

### 4. Restart Backend
```bash
cd backend
mvn spring-boot:run
```

## Current Application Configuration

### Backend (application.properties)
```properties
server.port=${PORT:8080}

# Database configuration with environment variable fallback
spring.datasource.url=${DB_URL:jdbc:h2:mem:peps_production;DB_CLOSE_DELAY=-1;MODE=MySQL}
spring.datasource.driverClassName=${DB_DRIVER:org.h2.Driver}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.database-platform=${DB_DIALECT:org.hibernate.dialect.H2Dialect}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

### Dependencies (pom.xml)
Both H2 and MySQL drivers are included:
- H2 (for local development)
- MySQL Connector J (for production/deployment)

## How It Works
- If environment variables are set → Uses MySQL
- If environment variables are NOT set → Falls back to H2
- This allows seamless switching between local development (H2) and production (MySQL)

## Testing MySQL Connection
After setting up MySQL, test the connection:
```bash
curl http://localhost:8080/api/dashboard
```

## Current System Status
- Backend: Running on http://localhost:8080 with H2
- Frontend: Running on http://localhost:3000
- Database: H2 in-memory (temporary for local development)
- MySQL Support: Configured and ready when MySQL is installed
