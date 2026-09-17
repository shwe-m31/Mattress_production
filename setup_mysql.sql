-- MySQL Setup Script for PEPS Production Database
-- Run this in MySQL command line or workbench

-- Create database
CREATE DATABASE IF NOT EXISTS peps_production CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - you can also use existing root user)
-- CREATE USER IF NOT EXISTS 'peps_user'@'localhost' IDENTIFIED BY 'peps_password';
-- GRANT ALL PRIVILEGES ON peps_production.* TO 'peps_user'@'localhost';
-- FLUSH PRIVILEGES;

-- If using root user, ensure root password is set correctly
-- The application expects: username=root, password=your_password
-- Update application.properties with your actual MySQL password

-- Verify database creation
SHOW DATABASES;

-- Verify user privileges (if you created a new user)
-- SHOW GRANTS FOR 'peps_user'@'localhost';
