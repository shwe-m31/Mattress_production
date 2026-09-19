CREATE DATABASE IF NOT EXISTS mattress_production;
USE mattress_production;

CREATE TABLE IF NOT EXISTS production_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_type VARCHAR(20) NOT NULL,
    variety VARCHAR(50),
    size VARCHAR(20) NOT NULL,
    production_line VARCHAR(50),
    start_time DATETIME,
    completion_time DATETIME NOT NULL,
    cycle_time DOUBLE,
    production_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_source VARCHAR(20),
    production_date DATE,
    shift VARCHAR(50),
    source_mode VARCHAR(50),
    PRIMARY KEY (id),
    INDEX idx_completion_time (completion_time),
    INDEX idx_status (status),
    INDEX idx_product_type (product_type),
    INDEX idx_production_line (production_line),
    INDEX idx_status_completion (status, completion_time),
    INDEX idx_production_date (production_date),
    INDEX idx_shift (shift)
);

