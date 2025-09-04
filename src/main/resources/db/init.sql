-- 1. 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS app_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE app_demo;

-- 3. 创建用户表（对应 User 实体）
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
                                      `username` varchar(50) NOT NULL COMMENT '用户名（唯一）',
    `password` varchar(100) NOT NULL COMMENT '密码（实际项目需加密存储）',
    `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一索引'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 4. 插入测试数据（可选）
INSERT IGNORE INTO `user` (username, password, nickname, create_time)
VALUES
('test1', '123456', '测试用户1', NOW()),
('test2', '654321', '测试用户2', NOW());