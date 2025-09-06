package com.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * User 实体类（对应 MySQL 中的 user 表）
 */
@Data // Lombok 自动生成 Getter/Setter/toString
@NoArgsConstructor // 无参构造
@AllArgsConstructor // 全参构造
@Entity // 标记为 JPA 实体
@Table(schema = "app_demo",name = "`user`") // 对应数据库表名：user
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键（MySQL 支持）
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50) // 用户名：非空、唯一
    private String username;

    @Column(name = "password", nullable = false, length = 100) // 密码（实际项目需加密，此处简化）
    private String password;

    @Column(name = "nickname", length = 50) // 昵称
    private String nickname;

    @Column(name = "create_time") // 创建时间
    private LocalDateTime createTime;

    // 新增/修改时自动填充时间（简化版：实际可通过 JPA 监听器实现）
    @PrePersist // 插入前执行
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

}

