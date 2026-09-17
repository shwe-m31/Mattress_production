# Database Configuration Guide

## Overview
The PEPS Production backend uses MySQL as the default database for persistent storage. H2 can be used for local development by setting environment variables.

## Option 1: MySQL Database (Default - Production)

### Advantages
- Persistent data storage
- Production-ready
- Data survives application restarts
- Better for long-term use and demonstrations

### Configuration
The application is configured to use MySQL by default:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Setup Instructions

### Advantages
- Persistent data storage
- Production-ready
- Data survives application restarts
- Better for long-term use

### Setup Instructions

#### 1. Install MySQL
- Download and install MySQL 8.0+ from https://dev.mysql.com/downloads/mysql/
- Or use Docker: `docker run --name mysql-standalone -e MYSQL_ROOT_PASSWORD=password -p 3306:3306 -d mysql:8.0`

#### 2. Create Database
Run the setup script or execute manually:
```bash
mysql -u root -p < setup_mysql.sql
```

Or manually:
```sql
CREATE DATABASE IF NOT EXISTS peps_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. Configure Database Credentials
Update the default credentials in `application.properties` or set environment variables:

**Option A: Update application.properties directly**
```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

**Option B: Use environment variables**
**Windows (PowerShell):**
```powershell
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"
```

**Linux/Mac:**
```bash
export DB_USERNAME="your_mysql_username"
export DB_PASSWORD="your_mysql_password"
```

#### 4. Run Application
```bash
cd backend
mvn spring-boot:run
```

### Troubleshooting MySQL Connection

#### Error: "Access denied for user 'root'@'localhost'"
**Solution:** Check your MySQL root password:
```bash
mysql -u root -p
# Enter your password
```

Update the environment variable with your actual password:
```bash
export DB_PASSWORD="your_actual_password"
```

#### Error: "Unknown database 'peps_production'"
**Solution:** Create the database:
```sql
CREATE DATABASE peps_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### Error: "MySQL server is not running"
**Solution:** Start MySQL service:
- Windows: Services → MySQL → Start
- Linux: `sudo systemctl start mysql`
- Mac: `brew services start mysql`

## Option 2: H2 Database (Local Development Alternative)

### Advantages
- No database setup required
- Runs in-memory
- Perfect for quick development and testing
- Application starts immediately

### When to Use H2
- Quick testing of simulation logic
- CI/CD pipelines
- When MySQL is not available
- Temporary development

### Configuration
To use H2 instead of MySQL, set these environment variables:

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:h2:mem:peps_production;DB_CLOSE_DELAY=-1;MODE=MySQL"
$env:DB_DRIVER="org.h2.Driver"
$env:DB_DIALECT="org.hibernate.dialect.H2Dialect"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD=""
```

**Linux/Mac:**
```bash
export DB_URL="jdbc:h2:mem:peps_production;DB_CLOSE_DELAY=-1;MODE=MySQL"
export DB_DRIVER="org.h2.Driver"
export DB_DIALECT="org.hibernate.dialect.H2Dialect"
export DB_USERNAME="sa"
export DB_PASSWORD=""
```

### How to Run with H2
```bash
# Set H2 environment variables
export DB_URL="jdbc:h2:mem:peps_production;DB_CLOSE_DELAY=-1;MODE=MySQL"
export DB_DRIVER="org.h2.Driver"
export DB_DIALECT="org.hibernate.dialect.H2Dialect"
export DB_USERNAME="sa"
export DB_PASSWORD=""

# Run application
cd backend
mvn spring-boot:run
```

### Important Notes
- H2 runs in-memory - data is lost when application stops
- This is fine for testing the deterministic simulation logic
- For production deployment, use MySQL (Option 1)

## Option 3: Docker MySQL (Easiest for MySQL Setup)

### Run MySQL in Docker
```bash
docker run --name peps-mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=peps_production \
  -p 3306:3306 \
  -d mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### Set Environment Variables (Optional)
The application uses MySQL by default, but you can override with environment variables:
```bash
export DB_URL="jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true"
export DB_DRIVER="com.mysql.cj.jdbc.Driver"
export DB_USERNAME="root"
export DB_PASSWORD="password"
```

### Run Application
```bash
cd backend
mvn spring-boot:run
```

### Stop MySQL Container
```bash
docker stop peps-mysql
docker rm peps-mysql
```

## Switching Between Databases

### From MySQL to H2
1. Set the H2 environment variables (see Option 2)
2. Restart the application
3. The application will use H2

### From H2 to MySQL
1. Unset the H2 environment variables:
   ```bash
   unset DB_URL
   unset DB_DRIVER
   unset DB_DIALECT
   unset DB_USERNAME
   unset DB_PASSWORD
   ```
2. Ensure MySQL is running and database is created
3. Restart the application
4. The application will use MySQL defaults

## Database Schema

### ProductionData Entity
The application automatically creates the following table structure:

```sql
CREATE TABLE production_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_type VARCHAR(50) NOT NULL,
    variety VARCHAR(50),
    size VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    production_line VARCHAR(50),
    start_time DATETIME NOT NULL,
    completion_time DATETIME NOT NULL,
    cycle_time DOUBLE,
    production_time DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL
);
```

### Indexes
Hibernate automatically creates indexes on:
- status
- completion_time
- product_type

## Data Persistence

### MySQL Behavior (Default)
- Data stored persistently
- Survives application restarts
- Production-ready
- Maintains historical data
- Demonstrates full system capabilities

### H2 Behavior (Alternative)
- Data stored in-memory
- Lost when application stops
- Perfect for testing simulation logic
- No data persistence across restarts

## Production Deployment (Render)

### Environment Variables in Render
Set these in your Render service dashboard:
```
DB_URL=jdbc:mysql://your-host:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_USERNAME=your_username
DB_PASSWORD=your_password
PORT=8080
```

### Render MySQL Database
1. Create a MySQL database in Render
2. Copy the connection details
3. Update environment variables with Render MySQL credentials
4. Deploy the application

## Verification

### Test Database Connection
```bash
# Test the application
curl http://localhost:8080/api/production/status

# Expected response:
{
  "connectionStatus": "Connected",
  "dataSource": "Simulated HMI/PLC",
  "simulatorActive": true,
  "totalRecords": 0
}
```

### Check Database Type
The application logs will show which database is being used:
- H2: "HHH000204: Processing PersistenceUnitInfo"
- MySQL: "HHH000400: Using dialect: org.hibernate.dialect.MySQLDialect"

## Recommendations

### For Development
- Use MySQL (default) for realistic testing
- Set up local MySQL instance
- Test restart persistence and data consistency
- Use H2 only for quick CI/CD testing

### For Production
- Use MySQL (default) for persistent storage
- Set up proper MySQL instance with backup
- Configure environment variables for security
- Ensure proper backup strategy

### For Demo/Presentation
- Use MySQL (default) to show data persistence
- Demonstrates restart persistence
- Shows historical data accumulation
- Realistic production simulation

## Current Status

The application is currently configured to:
- **Default to MySQL** for production-ready persistent storage
- **Support H2** via environment variables for development/testing
- **Maintain deterministic simulation logic** regardless of database choice
- **Work identically** with both databases (only persistence differs)

To run the application with MySQL (default):
1. Set up MySQL and create the database
2. Configure credentials in application.properties or environment variables
3. Run: `mvn spring-boot:run`

To run with H2 (alternative):
1. Set H2 environment variables (see Option 2)
2. Run: `mvn spring-boot:run`
