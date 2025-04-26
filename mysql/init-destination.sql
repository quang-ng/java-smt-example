-- Create Debezium user and grant privileges
CREATE USER IF NOT EXISTS 'mysqluser'@'%' IDENTIFIED BY 'mysqlpw';

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT
ON *.* TO 'mysqluser'@'%';

FLUSH PRIVILEGES;


CREATE DATABASE IF NOT EXISTS demo;
USE demo;

-- Create company table
CREATE TABLE IF NOT EXISTS company (
    id INT PRIMARY KEY,
    name VARCHAR(255)
);

-- Seed company data
INSERT INTO company (id, name) VALUES
    (10, 'Apple'),
    (20, 'Microsoft')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Create employee table
CREATE TABLE IF NOT EXISTS employee (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    company INT,
    FOREIGN KEY (company) REFERENCES company(id)
);
