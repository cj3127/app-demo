package com.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 3（OpenAPI）配置：访问地址 http://ip:8080/swagger-ui.html
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("App-Demo 接口文档")
                        .version("1.0.0")
                        .description("用户管理 CRUD 接口（适配 MySQL + Redis）"));
    }
}
