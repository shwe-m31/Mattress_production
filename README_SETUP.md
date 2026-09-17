# PEPS Mattress Production Backend - Setup Guide

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+ (required for default configuration)
- Git

### Local Development Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd Chakravyuha25_Ternion-main/sample2
```

2. **Setup MySQL Database**
```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE peps_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional, for security)
CREATE USER 'peps_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON peps_production.* TO 'peps_user'@'localhost';
FLUSH PRIVILEGES;
```

3. **Configure Database Credentials**
Update the default credentials in `backend/src/main/resources/application.properties`:
```properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

Or use environment variables:
```bash
# For Windows (PowerShell)
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"

# For Windows (Command Prompt)
set DB_USERNAME=your_mysql_username
set DB_PASSWORD=your_mysql_password

# For Linux/Mac
export DB_USERNAME=your_mysql_username
export DB_PASSWORD=your_mysql_password
```

4. **Run the Application**
```bash
cd backend
mvn spring-boot:run
```

5. **Verify Installation**
```bash
# Check health endpoint
curl http://localhost:8080/api/production/status

# Expected response includes:
# - connectionStatus: "Connected"
# - dataSource: "Simulated HMI/PLC"
# - simulatorActive: true
```

### Alternative: Using H2 Database
If you want to use H2 instead of MySQL for development:
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

## Deployment

### Render Deployment

1. **Create MySQL Database on Render**
   - Go to Render Dashboard
   - Create new MySQL database
   - Note the connection details

2. **Configure Environment Variables**
   In your Render service settings, add:
   ```
   DB_URL=jdbc:mysql://your-host:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
   DB_USERNAME=your_username
   DB_PASSWORD=your_password
   PORT=8080
   ```

3. **Deploy**
   ```bash
   # Push to GitHub
   git add .
   git commit -m "Deploy deterministic production simulation"
   git push origin main

   # Connect Render to your GitHub repository
   # Render will auto-deploy on push
   ```

### Docker Deployment

1. **Create Dockerfile** (if not exists)
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY backend/target/production-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

2. **Build and Run**
```bash
# Build JAR
cd backend
mvn clean package

# Build Docker image
docker build -t peps-production .

# Run container
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  peps-production
```

## Configuration

### Production Window
Edit `application.properties`:
```properties
production.start-time=09:00
production.end-time=17:00
```

### Production Targets
```properties
production.daily-target=500
production.weekly-target=2500
production.monthly-target=10000
```

### Production Rate
```properties
production.rate=65  # units per hour
```

### Simulation Interval
```properties
simulation.sync-interval=60000  # milliseconds
```

## Testing

### Manual Testing

1. **Test Production Progression**
```bash
# Get current production
curl http://localhost:8080/api/production/dashboard

# Wait 15 minutes
curl http://localhost:8080/api/production/dashboard

# Production should have increased
```

2. **Test Refresh Stability**
```bash
# Call twice within 1 minute
curl http://localhost:8080/api/production/dashboard
curl http://localhost:8080/api/production/dashboard

# Results should be nearly identical
```

3. **Test Restart Persistence**
```bash
# Note current production
curl http://localhost:8080/api/production/dashboard

# Restart application
# (Ctrl+C, then mvn spring-boot:run)

# Check production again
curl http://localhost:8080/api/production/dashboard

# Production should be the same
```

### Automated Testing

Create test script `test_api.sh`:
```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/production"

echo "Testing Dashboard API..."
curl -s "$BASE_URL/dashboard" | jq '.totalProduction'

echo "Testing Hourly API..."
curl -s "$BASE_URL/hourly" | jq '.spring[9]'

echo "Testing Status API..."
curl -s "$BASE_URL/status" | jq '.'

echo "All tests completed"
```

## Troubleshooting

### Database Connection Issues
```bash
# Test MySQL connection
mysql -h localhost -u root -p peps_production

# Check if database exists
SHOW DATABASES;

# Check connection string
echo $DB_URL
```

### Timezone Issues
```bash
# Check system timezone
date

# Check Java timezone
java -Duser.timezone=Asia/Kolkata -version

# Verify application timezone
curl http://localhost:8080/api/production/status | jq '.lastUpdateTime'
```

### Port Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Build Issues
```bash
# Clean and rebuild
cd backend
mvn clean install -U

# Skip tests if needed
mvn clean package -DskipTests
```

## Monitoring

### Application Logs
```bash
# View logs in real-time
tail -f backend/logs/application.log

# Check for errors
grep ERROR backend/logs/application.log
```

### Database Monitoring
```sql
-- Check production count
SELECT COUNT(*) FROM production_data 
WHERE status = 'COMPLETED' 
AND DATE(completion_time) = CURDATE();

-- Check recent events
SELECT * FROM production_data 
ORDER BY completion_time DESC 
LIMIT 10;

-- Check hourly distribution
SELECT HOUR(completion_time) as hour, 
       SUM(quantity) as total 
FROM production_data 
WHERE DATE(completion_time) = CURDATE()
GROUP BY HOUR(completion_time)
ORDER BY hour;
```

## Maintenance

### Database Backup
```bash
# Backup database
mysqldump -u root -p peps_production > backup_$(date +%Y%m%d).sql

# Restore database
mysql -u root -p peps_production < backup_20250917.sql
```

### Log Rotation
Configure log rotation in `application.properties`:
```properties
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
logging.logback.rollingpolicy.total-size-cap=1GB
```

### Performance Tuning
```properties
# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JPA
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

## Security

### Best Practices
1. Never commit database credentials
2. Use strong passwords
3. Restrict database user permissions
4. Enable SSL for production databases
5. Use environment variables for secrets
6. Regular security updates

### Environment Variables
```bash
# Use secrets management
# Render: Use Render Dashboard
# Docker: Use Docker secrets
# Kubernetes: Use Kubernetes secrets
```

## Support

For issues:
1. Check logs: `backend/logs/application.log`
2. Verify configuration: `application.properties`
3. Test database connection
4. Review implementation guide: `IMPLEMENTATION_GUIDE.md`
5. Check GitHub issues

## License

This project is part of the Chakravyuha25 Ternion hackathon.
