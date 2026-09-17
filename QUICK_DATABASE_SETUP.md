# Quick Database Setup Guide

## Option 1: Using MySQL Workbench (Recommended)

1. Open MySQL Workbench
2. Connect to your MySQL server using:
   - Host: localhost
   - Username: root
   - Password: Mysql@12345
3. Run this SQL query in the query editor:

```sql
CREATE DATABASE IF NOT EXISTS mattress_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

4. Click the lightning bolt to execute the query
5. Verify the database was created by checking the Schemas panel

## Option 2: Using Command Line

Open Command Prompt (cmd) and run:

```cmd
mysql -u root -pMysql@12345 -e "CREATE DATABASE IF NOT EXISTS mattress_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

## Option 3: Automatic Database Creation

The application.properties has been updated with `createDatabaseIfNotExist=true`, which should automatically create the database when the application starts.

## After Database Creation

Once the database is created, run the application again:

```bash
cd backend
mvn spring-boot:run
```

The application should now start successfully and automatically create the required tables.