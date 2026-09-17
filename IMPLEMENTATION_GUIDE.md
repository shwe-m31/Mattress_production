# PEPS Mattress Production Backend - Implementation Guide

## Overview
This implementation refactors the PEPS Mattress Production backend to use deterministic, time-based production simulation with MySQL persistence, replacing the previous random simulation model.

## Key Changes Made

### 1. Database Migration (H2 → MySQL)
**File:** `backend/src/main/resources/application.properties`

- Replaced H2 in-memory database with MySQL
- Added MySQL JDBC driver configuration
- Configured timezone to Asia/Kolkata for consistent time handling
- Added production window and target configurations

**Configuration:**
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true}
spring.datasource.driverClassName=${DB_DRIVER:com.mysql.cj.jdbc.Driver}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:password}
spring.jpa.database-platform=${DB_DIALECT:org.hibernate.dialect.MySQLDialect}
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata
```

### 2. Production Time Service (NEW)
**File:** `backend/src/main/java/com/peps/production/service/ProductionTimeService.java`

- Manages production window (09:00-17:00)
- Calculates elapsed production time
- Determines expected production based on time progression
- Provides centralized configuration for production targets

**Key Features:**
- Configurable production window via `production.start-time` and `production.end-time`
- Linear production model with small deterministic variation
- Time-based expected production calculation
- Configurable daily target and production rate

### 3. Production Simulation Service (NEW)
**File:** `backend/src/main/java/com/peps/production/service/ProductionSimulationService.java`

- Replaces random event generation with deterministic time-based simulation
- Implements idempotent production synchronization
- Ensures monotonic production progression
- Uses deterministic random seeding for consistent product distribution

**Key Features:**
- `synchronizeProduction()`: Idempotent method to sync production with expected state
- Only generates missing production events (never duplicates)
- Uses date-based seeding for consistent historical data
- Maintains 58% Spring / 42% Hypnos product distribution
- Realistic cycle times (3.5-5.5 minutes)

### 4. Updated Production Simulator Service
**File:** `backend/src/main/java/com/peps/production/service/ProductionSimulatorService.java`

- Removed random 10-second event generation
- Replaced with scheduled synchronization (60-second intervals)
- Initial synchronization on application startup
- Manual simulation endpoint for testing

**Changes:**
- Removed `@Scheduled` random event generation
- Added production synchronization on startup
- Added manual simulation method with time validation
- Changed from 10-second to 60-second sync interval

### 5. Updated Dashboard Service
**File:** `backend/src/main/java/com/peps/production/service/DashboardService.java`

- Made GET API read-only with pre-synchronization
- Updated efficiency calculation to use time-based expectations
- Ensures all dashboard data comes from persisted records

**Changes:**
- Calls `simulationService.synchronizeProduction()` before returning data
- Calculates efficiency based on actual vs expected production for elapsed time
- All data derived from MySQL production records

### 6. Updated Production Controller
**File:** `backend/src/main/java/com/peps/production/controller/ProductionController.java`

- Made all GET endpoints read-only
- Centralized production targets (daily: 500, weekly: 2500, monthly: 10000)
- Updated hourly production to exclude future hours
- Enhanced production status with current/expected production
- Improved downtime calculation using production window

**Changes:**
- Hourly: Only includes hours up to current time
- Daily/Weekly/Monthly: Use centralized targets
- Status: Added currentProduction and expectedProduction fields
- History: READ-ONLY, no synchronization needed

### 7. Enhanced Repository
**File:** `backend/src/main/java/com/peps/production/repository/ProductionRepository.java`

- Added aggregate queries for production totals
- Efficient sum operations instead of streaming all records

**New Methods:**
- `sumQuantityByStatusAndCompletionTimeBetween()`
- `sumQuantityByStatusAndProductTypeAndCompletionTimeBetween()`

### 8. Timezone Configuration
**File:** `backend/src/main/java/com/peps/production/PepsProductionApplication.java`

- Set default JVM timezone to Asia/Kolkata
- Ensures consistent time handling across application

### 9. Enhanced DTO
**File:** `backend/src/main/java/com/peps/production/dto/ProductionStatusResponse.java`

- Added `currentProduction` field
- Added `expectedProduction` field
- Provides real-time production status information

## Configuration Properties

### Production Window
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

### Simulation
```properties
simulation.sync-interval=60000  # milliseconds (60 seconds)
```

## Database Setup

### MySQL Database Creation
```sql
CREATE DATABASE peps_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Environment Variables
```bash
# For local development
export DB_URL=jdbc:mysql://localhost:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
export DB_USERNAME=root
export DB_PASSWORD=your_password

# For deployment (Render example)
export DB_URL=jdbc:mysql://your-host:3306/peps_production?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

## Running the Application

### Local Development
1. Ensure MySQL is running and database is created
2. Set environment variables or update `application.properties`
3. Run the application:
```bash
cd backend
mvn spring-boot:run
```

### Production Deployment
1. Set environment variables in your deployment platform
2. Build the application:
```bash
mvn clean package
```
3. Deploy the JAR file to your platform

## API Behavior

### GET Endpoints (READ-ONLY)
All GET endpoints are now read-only and calculate data from persisted MySQL records:

- `GET /api/production/dashboard` - Synchronizes production, then returns dashboard data
- `GET /api/production/hourly` - Returns hourly production (future hours = 0)
- `GET /api/production/daily` - Returns 14 days of daily production
- `GET /api/production/weekly` - Returns 8 weeks of weekly production
- `GET /api/production/monthly` - Returns 12 months of monthly production
- `GET /api/production/recent` - Returns recent production events
- `GET /api/production/status` - Returns system status with production info
- `GET /api/production/history` - Returns historical production data

### Refresh Stability
Multiple requests within seconds return identical data:
- 14:30:00 → 350 units
- 14:30:10 → 350 units
- 14:30:30 → 351 units (gradual increase)

### Time-Based Progression
Production increases gradually with time:
- 09:00 → 0 units
- 10:00 → ~65 units
- 12:00 → ~195 units
- 14:00 → ~325 units
- 17:00 → ~500 units

### Restart Persistence
After backend restart:
- Production count remains consistent
- No data loss
- No random regeneration
- Continues from previous state

### New Day Handling
- Previous day's data remains in MySQL
- New day starts at 0 production
- Before 09:00 → 0 units
- After 09:00 → gradual increase

## Test Scenarios

### Test 1: Before Production Start
**Time:** 08:30
**Expected:** Production = 0
**Verification:** Call dashboard API, verify totalProduction = 0

### Test 2: Production Start
**Time:** 09:00
**Expected:** Production ≈ 0
**Verification:** Call dashboard API, verify small production count

### Test 3: Gradual Increase
**Time:** 10:00 < 11:00 < 12:00
**Expected:** Production increases monotonically
**Verification:** Compare production counts at different times

### Test 4: Refresh Stability
**Time:** Within 1 minute
**Expected:** Nearly identical totals
**Verification:** Call dashboard API twice within 1 minute, compare results

### Test 5: 15-Minute Progression
**Time:** 14:00, 14:15, 14:30
**Expected:** Monotonic increase
**Verification:** Production at 14:00 ≤ 14:15 ≤ 14:30

### Test 6: No Future Data
**Time:** 14:30
**Expected:** Future hours = 0
**Verification:** Check hourly API, hours 15-17 should be 0

### Test 7: Restart Persistence
**Action:** Restart backend
**Expected:** Production count remains consistent
**Verification:** Compare production before and after restart

### Test 8: New Day
**Time:** After midnight
**Expected:** Previous day preserved, new day starts at 0
**Verification:** Check daily API for both days

### Test 9: Product Totals
**Expected:** Spring + Hypnos = Total
**Verification:** Verify dashboard totals match sum of product types

### Test 10: Size Totals
**Expected:** King + Queen + Double + Single = Total
**Verification:** Verify size breakdown matches total

### Test 11: Hourly Totals
**Expected:** Sum of hourly = daily total
**Verification:** Compare hourly sum with dashboard total

### Test 12: MySQL Persistence
**Action:** Stop/restart backend
**Expected:** Production records remain
**Verification:** Query MySQL database directly

## Deterministic Behavior

### Seeding
- Historical data uses date-based seeding: `new Random(date.toEpochDay())`
- Same date always produces same historical dataset
- No random regeneration on restart

### Product Distribution
- Fixed ratio: 58% Spring, 42% Hypnos
- Deterministic variety selection based on seed
- Consistent size distribution

### Time-Based Variation
- Small deterministic variation based on time
- Formula: `timeBasedVariation = minute * 0.1 + second * 0.01`
- Ensures same time produces same result

## Monotonic Production Guarantee

### Implementation
- Production never decreases on same day
- Only creates additional events, never removes
- Database query ensures actual production count
- Synchronization only adds missing events

### Safety Checks
- Validates completion time ≤ current time
- Ensures events within production window
- Logs warnings if actual > expected

## Idempotency

### Synchronization Method
```java
synchronizeProduction()
synchronizeProduction()
synchronizeProduction()
```
- Calling multiple times produces same result
- Only creates missing events
- Never duplicates existing events
- Safe for scheduled execution

## Error Handling

### Database Connection
- Logs errors during synchronization
- Continues operation if sync fails
- Graceful degradation

### Time Validation
- Prevents future completion times
- Validates production window
- Logs warnings for anomalies

## Performance Considerations

### Database Queries
- Added aggregate queries for efficient totals
- Reduced data streaming
- Indexed fields: status, completionTime, productType

### Synchronization Interval
- 60-second default interval
- Configurable via `simulation.sync-interval`
- Balances freshness with performance

## Security

### Credentials
- Never commit real database credentials
- Use environment variables
- Default values for local development only

### Data Validation
- Validates all input parameters
- Prevents SQL injection via JPA
- Time-based validation prevents future data

## Monitoring

### Logging
- INFO level for synchronization events
- DEBUG level for detailed event generation
- WARN level for anomalies

### Status Endpoint
- Returns current production count
- Returns expected production count
- Shows simulator status
- Total records count

## Troubleshooting

### Production Not Increasing
1. Check current time is within production window (09:00-17:00)
2. Verify MySQL connection
3. Check application logs for errors
4. Verify synchronization is running

### Incorrect Production Count
1. Check timezone configuration (Asia/Kolkata)
2. Verify production window settings
3. Check for database inconsistencies
4. Review synchronization logs

### Future Hours Showing Data
1. Verify current system time
2. Check timezone configuration
3. Review hourly API implementation
4. Ensure synchronization is working

### Restart Loses Data
1. Verify MySQL is persistent (not in-memory)
2. Check database connection string
3. Verify Hibernate DDL settings
4. Check for database errors on startup

## Migration from Previous Version

### Data Migration
- Existing H2 data will be lost (expected for simulation)
- MySQL will start fresh
- Production will build up from current time
- Historical data can be seeded if needed

### API Compatibility
- All existing endpoints preserved
- Response structures enhanced (backward compatible)
- Frontend should work without changes
- Additional fields in status response

### Configuration Migration
- Update database configuration
- Add production window settings
- Add target configurations
- Update environment variables

## Future Enhancements

### Potential Improvements
- Add actual PLC/HMI integration
- Implement real downtime tracking
- Add production pause/resume functionality
- Implement shift-based targets
- Add production efficiency alerts
- Create admin interface for configuration

### Database Optimizations
- Add composite indexes on frequently queried fields
- Implement database partitioning for large datasets
- Add connection pooling configuration
- Consider read replicas for reporting

### Monitoring
- Add Prometheus metrics
- Implement health checks
- Create production dashboards
- Add alerting for anomalies

## Support

For issues or questions:
1. Check application logs
2. Verify configuration settings
3. Test with provided scenarios
4. Review this implementation guide
5. Check database state directly

## Conclusion

This implementation provides a robust, deterministic production simulation system that:
- Uses actual system time for production calculations
- Maintains data persistence across restarts
- Ensures refresh stability
- Provides realistic time-based progression
- Maintains backward API compatibility
- Supports future real PLC integration

The system is now production-ready for demonstration purposes while maintaining the flexibility to integrate with real manufacturing systems.
