CREATE DATABASE IF NOT EXISTS peps_production_db;
USE peps_production_db;

CREATE TABLE IF NOT EXISTS production_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_type VARCHAR(20) NOT NULL,
    size VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    completion_time DATETIME NOT NULL,
    production_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_production_completion (completion_time),
    INDEX idx_production_status (status)
);
