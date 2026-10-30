-- Nova instalacia: spustite v MySQL / phpMyAdmin.
CREATE DATABASE IF NOT EXISTS employee CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE employee;
CREATE TABLE IF NOT EXISTS employee (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(25) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    salary INT NOT NULL,
    full_time VARCHAR(30) NOT NULL
);
