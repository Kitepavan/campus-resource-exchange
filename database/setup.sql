-- MySQL 8 setup for the Campus Resource Exchange.
-- Run as root: sudo mysql < database/setup.sql
-- Replace the password before running this file in any real environment.

CREATE DATABASE IF NOT EXISTS campus_exchange
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'campus_app'@'localhost'
  IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';

GRANT ALL PRIVILEGES ON campus_exchange.* TO 'campus_app'@'localhost';
FLUSH PRIVILEGES;
