package com.xjtu.iron.concurrency.demo;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolManager;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolSnapshot;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * ThreadPoolManager 使用示例。
 *
 * <p>这个 Controller 用于演示线程池运行时诊断和手工扩缩容能力。</p>
 */
@RestController
@RequestMapping("/demo/thread-pool-manager")
public class ThreadPoolManagerDemoController {

    /**
     * 业务查询线程池名称。
     */
    private static final String BIZ_QUERY_EXECUTOR = "biz-query-pool";

    /**
     * 线程池运行时管理器。
     */
    private final ThreadPoolManager threadPoolManager;

    /**
     * 创建线程池管理示例 Controller。
     *
     * @param threadPoolManager 线程池运行时管理器
     */
    public ThreadPoolManagerDemoController(ThreadPoolManager threadPoolManager) {
        this.threadPoolManager = threadPoolManager;
    }

    /**
     * snapshot 示例：查看单个线程池的运行时快照。
     *
     * @return 单个线程池快照
     */
    @GetMapping("/snapshot")
    public ThreadPoolSnapshot snapshot() {
        return threadPoolManager.snapshot(BIZ_QUERY_EXECUTOR);
    }

    /**
     * snapshots 示例：查看所有线程池的运行时快照。
     *
     * @return 所有线程池快照
     */
    @GetMapping("/snapshots")
    public Map<String, ThreadPoolSnapshot> snapshots() {
        return threadPoolManager.snapshots();
    }

    /**
     * resize 示例：调整 corePoolSize 和 maximumPoolSize。
     *
     * @return 调整后的线程池快照
     */
    @GetMapping("/resize")
    public ThreadPoolSnapshot resize() {
        return threadPoolManager.resize(BIZ_QUERY_EXECUTOR, 8, 32);
    }

    /**
     * update 示例：完整更新运行时可变配置。
     *
     * @return 更新后的线程池快照
     */
    @GetMapping("/update")
    public ThreadPoolSnapshot update() {
        ThreadPoolUpdateRequest request = new ThreadPoolUpdateRequest();
        request.setCorePoolSize(8);
        request.setMaximumPoolSize(32);
        request.setKeepAliveTime(Duration.ofSeconds(30));
        request.setAllowCoreThreadTimeout(false);
        request.setRejectionPolicy(RejectionPolicy.BLOCKING_WAIT);
        request.setRejectionWaitTime(Duration.ofMillis(100));
        request.setPrestartAllCoreThreads(true);

        return threadPoolManager.update(BIZ_QUERY_EXECUTOR, request);
    }
}
