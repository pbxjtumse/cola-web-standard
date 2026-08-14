package com.xjtu.iron.transaction.demo.mybatis.application;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyBatis XML 事务 Demo 启动类。
 */
@SpringBootApplication(scanBasePackages = "com.xjtu.iron.transaction.demo.mybatis")
@MapperScan("com.xjtu.iron.transaction.demo.mybatis.mapper")
public class MybatisTransactionDemoApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 后，Starter 会基于 DataSourceTransactionManager 自动装配 TransactionExecutor。
        SpringApplication.run(MybatisTransactionDemoApplication.class, args);
    }
}
