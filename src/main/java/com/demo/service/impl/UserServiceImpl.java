package com.demo.service.impl;

import com.demo.entity.User;
import com.demo.repository.UserRepository;
import com.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User 业务实现（含 Redis 缓存注解）
 */
@Service
@RequiredArgsConstructor // Lombok 自动注入依赖（替代 @Autowired）
public class UserServiceImpl implements UserService {

    // 注入数据访问层
    private final UserRepository userRepository;

    /**
     * 新增用户（无缓存：新增后需清理列表缓存）
     */
    @Override
    @Transactional // 事务管理（确保数据一致性）
    @CacheEvict(value = "userList", allEntries = true) // 新增后清空用户列表缓存
    public User save(User user) {
        // 校验用户名是否已存在（避免唯一键冲突）
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在：" + user.getUsername());
        }
        return userRepository.save(user);
    }

    /**
     * 删除用户（删除后清理对应缓存）
     */
    @Override
    @Transactional
    @CacheEvict(value = {"user", "userList"}, allEntries = true) // 清空单个用户和列表缓存
    public void deleteById(Long id) {
        // 校验用户是否存在
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在：ID=" + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * 更新用户（更新后更新缓存）
     */
    @Override
    @Transactional
    @CachePut(value = "user", key = "#user.id") // 更新缓存（key 为用户 ID）
    @CacheEvict(value = "userList", allEntries = true) // 更新后清空列表缓存
    public User update(User user) {
        // 校验用户是否存在
        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("用户不存在：ID=" + user.getId());
        }
        // 校验用户名是否冲突（若修改了用户名）
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new RuntimeException("用户名已存在：" + user.getUsername());
        }
        return userRepository.save(user);
    }

    /**
     * 根据 ID 查询用户（缓存：key 为用户 ID）
     */
    @Override
    @Cacheable(value = "user", key = "#id", unless = "#result == null") // 缓存非 null 结果
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 查询所有用户（缓存：key 固定为 "all"）
     */
    @Override
    @Cacheable(value = "userList", key = "'all'", unless = "#result.isEmpty()") // 缓存非空列表
    public List<User> listAll() {
        return userRepository.findAll();
    }

    /**
     * 根据用户名查询用户（无缓存：用户名查询频率低，且易变化）
     */
    @Override
    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}