package com.xjtu.iron.retry.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 重试组件一期演示应用。
 */
@SpringBootApplication
public class RetryDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryDemoApplication.class, args);
    }
}
