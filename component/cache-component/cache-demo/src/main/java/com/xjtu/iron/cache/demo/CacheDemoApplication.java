package com.xjtu.iron.cache.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 缓存组件 Demo 启动类。
 *
 * <p>main 方法只负责启动 Spring Boot，不应该在 main 方法里写缓存调用逻辑。
 * 缓存调用应该放在 Controller -> Service -> CacheClient 这条链路里。</p>
 */
@SpringBootApplication
public class CacheDemoApplication {

    /** Spring Boot 应用入口。 */
    public static void main(String[] args) {
        SpringApplication.run(CacheDemoApplication.class, args);
    }
}
