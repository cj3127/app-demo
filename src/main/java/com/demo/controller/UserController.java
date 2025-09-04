package com.demo.controller;

import com.demo.entity.User;
import com.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户管理接口（RESTful 风格）
 * 访问地址：http://ip:8080/api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理接口", description = "提供用户 CRUD 操作") // Swagger 标签
public class UserController {

    private final UserService userService;

    /**
     * 1. 新增用户
     */
    @PostMapping
    @Operation(summary = "新增用户", description = "传入用户名、密码、昵称，创建新用户")
    public ResponseEntity<User> save(
            @Parameter(description = "用户信息（id 无需传入，自动生成）") @RequestBody User user) {
        User savedUser = userService.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED); // 201 状态码
    }

    /**
     * 2. 根据 ID 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户 ID 删除用户")
    public ResponseEntity<Void> deleteById(
            @Parameter(description = "用户 ID") @PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build(); // 204 状态码（无返回值）
    }

    /**
     * 3. 根据 ID 更新用户
     */
    @PutMapping
    @Operation(summary = "更新用户", description = "传入完整用户信息（含 id），全量更新")
    public ResponseEntity<User> update(
            @Parameter(description = "用户信息（id 必须传入）") @RequestBody User user) {
        User updatedUser = userService.update(user);
        return ResponseEntity.ok(updatedUser); // 200 状态码
    }

    /**
     * 4. 根据 ID 查询用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询单个用户", description = "根据用户 ID 查询用户详情")
    public ResponseEntity<User> getById(
            @Parameter(description = "用户 ID") @PathVariable Long id) {
        Optional<User> user = userService.getById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()); // 不存在返回 404
    }

    /**
     * 5. 查询所有用户
     */
    @GetMapping
    @Operation(summary = "查询所有用户", description = "返回所有用户列表（带 Redis 缓存）")
    public ResponseEntity<List<User>> listAll() {
        List<User> users = userService.listAll();
        return ResponseEntity.ok(users);
    }

    /**
     * 6. 根据用户名查询用户
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查询", description = "传入用户名，查询对应用户")
    public ResponseEntity<User> getByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        Optional<User> user = userService.getByUsername(username);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}