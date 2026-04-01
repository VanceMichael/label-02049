CREATE DATABASE IF NOT EXISTS concert_ticket DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE concert_ticket;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(200) NOT NULL,
    `email` VARCHAR(100) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通用户,1=管理员',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=正常,1=禁用',
    `login_fail_count` INT NOT NULL DEFAULT 0,
    `lock_until` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 演唱会表
CREATE TABLE IF NOT EXISTS `concert` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `artist` VARCHAR(100) NOT NULL,
    `venue` VARCHAR(200) NOT NULL,
    `city` VARCHAR(50) NOT NULL,
    `show_time` DATETIME NOT NULL,
    `poster_url` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=未开始,1=售票中,2=已结束,3=已下架',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 票档表
CREATE TABLE IF NOT EXISTS `ticket_tier` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `concert_id` BIGINT NOT NULL,
    `tier_name` VARCHAR(50) NOT NULL COMMENT '普通票/VIP票',
    `price` DECIMAL(10,2) NOT NULL,
    `total_stock` INT NOT NULL DEFAULT 0,
    `available_stock` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_concert_id` (`concert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(64) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `concert_id` BIGINT NOT NULL,
    `ticket_tier_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `total_price` DECIMAL(10,2) NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=待支付,1=已支付,2=已取消,3=已退款,4=已完成',
    `expire_at` DATETIME NOT NULL COMMENT '支付截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_order_no` (`order_no`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单日志表
CREATE TABLE IF NOT EXISTS `order_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `action` VARCHAR(50) NOT NULL,
    `operator` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT DEFAULT NULL COMMENT 'NULL=全员通知',
    `type` VARCHAR(20) NOT NULL COMMENT 'SYSTEM/PERSONAL',
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0=未读,1=已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员账号通过 DataInitializer 在应用启动时自动创建 (admin / admin123)
