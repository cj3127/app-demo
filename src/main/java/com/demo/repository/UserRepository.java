package com.demo.repository;

import com.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 数据访问层（JPA 自动实现 CRUD 方法，无需写 SQL）
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 自定义查询：根据用户名查询用户（JPA 按方法名自动生成 SQL）
    Optional<User> findByUsername(String username);

    // 自定义查询：根据用户名判断是否存在
    boolean existsByUsername(String username);
}