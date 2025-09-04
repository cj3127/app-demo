package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // 开启 Redis 缓存

/**
 * 应用启动类（入口）
 */
@SpringBootApplication
@EnableCaching // 启用缓存（Redis）
public class AppDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppDemoApplication.class, args);
    }

}