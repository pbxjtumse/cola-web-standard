package com.xjtu.iron.transaction.demo.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xjtu.iron.transaction.demo.mybatis")
public class MybatisTransactionDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisTransactionDemoApplication.class, args);
    }
}
