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


