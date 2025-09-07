-- 1. 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS app_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE app_demo;

-- 3. 创建房屋表（对应 House 实体）
CREATE TABLE IF NOT EXISTS `house` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房屋ID（主键）',
                                       `行政区` varchar(255) NOT NULL COMMENT '行政区',
    `所属小区` varchar(255) NOT NULL COMMENT '所属小区',
    `房屋户型` varchar(255) DEFAULT NULL COMMENT '房屋户型',
    `房屋朝向` varchar(255) DEFAULT NULL COMMENT '房屋朝向',
    `所在楼层` int DEFAULT NULL COMMENT '所在楼层',
    `装修程度` varchar(255) DEFAULT NULL COMMENT '装修程度',
    `配套电梯` varchar(255) DEFAULT NULL COMMENT '配套电梯（有/无）',
    `建筑面积` int DEFAULT NULL COMMENT '建筑面积（平方米）',
    `房屋总价` int DEFAULT NULL COMMENT '房屋总价（元）',
    `建造年代` int DEFAULT NULL COMMENT '建造年代',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋表';

-- 4. 插入测试数据
INSERT IGNORE INTO `house` (行政区, 所属小区, 房屋户型, 房屋朝向, 所在楼层, 装修程度, 配套电梯, 建筑面积, 房屋总价, 建造年代)
VALUES
('朝阳区', '阳光小区', '3室2厅', '南北通透', 15, '精装修', '有', 120, 8000000, 2010),
('海淀区', '学府花园', '2室1厅', '朝南', 8, '简装修', '有', 89, 6500000, 2005);
