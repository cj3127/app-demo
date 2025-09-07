package com.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * House 实体类（对应 MySQL 中的 house 表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "app_demo", name = "house") // 对应数据库表
public class House implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    private Long id; // 新增主键字段（表结构未提及，实际表通常需要主键）

    @Column(name = "行政区", nullable = false, length = 255)
    private String district; // 行政区

    @Column(name = "所属小区", nullable = false, length = 255)
    private String community; // 所属小区

    @Column(name = "房屋户型", length = 255)
    private String houseType; // 房屋户型

    @Column(name = "房屋朝向", length = 255)
    private String orientation; // 房屋朝向

    @Column(name = "所在楼层")
    private Integer floor; // 所在楼层

    @Column(name = "装修程度", length = 255)
    private String decoration; // 装修程度

    @Column(name = "配套电梯", length = 255)
    private String elevator; // 配套电梯（可存储"有"或"无"）

    @Column(name = "建筑面积")
    private Integer area; // 建筑面积（单位：平方米）

    @Column(name = "房屋总价")
    private Integer totalPrice; // 房屋总价（单位：元）

    @Column(name = "建造年代")
    private Integer buildYear; // 建造年代
}
