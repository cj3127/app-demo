package com.demo.service;

import com.demo.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * User 业务接口
 */
public interface UserService {

    // 1. 新增用户
    User save(User user);

    // 2. 根据 ID 删除用户
    void deleteById(Long id);

    // 3. 根据 ID 更新用户（全量更新）
    User update(User user);

    // 4. 根据 ID 查询用户（缓存）
    Optional<User> getById(Long id);

    // 5. 查询所有用户（缓存）
    List<User> listAll();

    // 6. 根据用户名查询用户
    Optional<User> getByUsername(String username);

}
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigTestController {

    // 打印 Hibernate default_schema 配置
    @Value("${spring.jpa.properties.hibernate.default_schema:未配置}")
    private String hibernateDefaultSchema;

    // 打印数据源 URL（确认是否连接到 app_demo 库）
    @Value("${spring.datasource.url:未配置}")
    private String dataSourceUrl;

    // 打印 Hikari connection-init-sql 配置
    @Value("${spring.datasource.hikari.connection-init-sql:未配置}")
    private String hikariInitSql;

    @GetMapping("/test/config")
    public String testConfig() {
        return "运行时配置：\n" +
                "1. hibernate.default_schema: " + hibernateDefaultSchema + "\n" +
                "2. 数据源 URL: " + dataSourceUrl + "\n" +
                "3. hikari.connection-init-sql: " + hikariInitSql;
    }
}
