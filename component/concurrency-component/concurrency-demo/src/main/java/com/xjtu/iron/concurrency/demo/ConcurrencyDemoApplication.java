package com.xjtu.iron.concurrency.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 并行组件 Demo 启动类。
 *
 * <p>用于验证 concurrency-spring-boot-starter 自动装配是否正常。</p>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ConcurrencyDemoApplication {

    /**
     * Spring Boot 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConcurrencyDemoApplication.class, args);
    }
}
