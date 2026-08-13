package com.xjtu.iron.idempotent.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 幂等组件独立演示应用。
 *
 * <p>该模块只用于验证 Starter 自动装配、Redis/JDBC Repository、短期窗口和 DURABLE 状态机。
 * 真正业务系统只需要依赖 {@code idempotent-starter}，不需要依赖 demo。</p>
 */
@SpringBootApplication
public class IdempotencyDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotencyDemoApplication.class, args);
    }
}
