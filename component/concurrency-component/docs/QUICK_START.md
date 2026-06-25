# QUICK_START：快速开始

## 本文适合谁看

适合第一次接入并行组件，希望快速跑起来的人。

## 读完你会知道什么

- 如何引入依赖。
- 如何写配置。
- 如何提交第一个任务。
- 如何加超时和 fallback。
- 如何取消任务。
- 如何验证线程池是否生效。

## 目录

- [1. 引入依赖](#1-引入依赖)
- [2. 添加配置](#2-添加配置)
- [3. 注入 AsyncExecutor](#3-注入-asyncexecutor)
- [4. 提交第一个任务](#4-提交第一个任务)
- [5. 带返回值任务](#5-带返回值任务)
- [6. 带超时和 fallback 的任务](#6-带超时和-fallback-的任务)
- [7. 可取消任务](#7-可取消任务)
- [8. 查询任务状态](#8-查询任务状态)
- [9. 验证线程池](#9-验证线程池)
- [10. 常见启动问题](#10-常见启动问题)

## 1. 引入依赖

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>concurrency-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 2. 添加配置

最小配置：

```yaml
xjtu:
  iron:
    concurrency:
      enabled: true
      thread-pools:
        default:
          core-pool-size: 4
          max-pool-size: 8
          queue-capacity: 200
          keep-alive-time: 60s
          thread-name-prefix: default-async-
          queue-type: BOUNDED_ARRAY_BLOCKING_QUEUE
          rejection-policy: ABORT
          wait-for-tasks-to-complete-on-shutdown: true
          await-termination: 10s
```

建议第一个版本只配置一个 `default` 线程池，跑通后再根据业务场景拆分多个线程池。

## 3. 注入 AsyncExecutor

```java
@RequiredArgsConstructor
@Service
public class UserFacade {

    private final AsyncExecutor asyncExecutor;
}
```

## 4. 提交第一个任务

```java
public void sendLog(LogDTO log) {
    asyncExecutor.execute(
            "default",
            "sendLog",
            () -> logService.send(log)
    );
}
```

这个方法适合不关心结果的异步任务。

## 5. 带返回值任务

```java
public UserDTO queryUser(String userId) {
    CompletableFuture<UserDTO> future = asyncExecutor.supply(
            "default",
            "queryUser",
            () -> userService.query(userId)
    );

    return future.join();
}
```

如果需要并行查询多个数据：

```java
CompletableFuture<UserDTO> userFuture = asyncExecutor.supply(
        "default",
        "queryUser",
        () -> userService.query(userId)
);

CompletableFuture<AccountDTO> accountFuture = asyncExecutor.supply(
        "default",
        "queryAccount",
        () -> accountService.query(userId)
);

return UserSummaryDTO.of(
        userFuture.join(),
        accountFuture.join()
);
```

## 6. 带超时和 fallback 的任务

```java
public UserDTO queryUserWithFallback(String userId) {
    return asyncExecutor.submit(
            AsyncTask.of(
                    "default",
                    "queryUser",
                    () -> userService.query(userId)
            )
            .timeout(Duration.ofSeconds(2))
            .fallback(error -> UserDTO.empty(userId))
    ).join();
}
```

含义：

```text
2 秒内成功：返回真实用户。
2 秒内失败、超时、被拒绝：执行 fallback，返回空用户。
fallback 也失败：最终 Future 异常完成。
```

## 7. 可取消任务

```java
TaskHandle<String> handle = asyncExecutor.submitHandle(
        AsyncTask.of(
                "default",
                "longTask",
                () -> longTaskService.execute()
        )
);

TaskCancelResult result = handle.cancel(true);
```

说明：

```text
cancel(true) 会尝试中断运行线程。
但 Java interrupt 是协作式，不是强杀。
底层代码需要自己响应中断。
```

## 8. 查询任务状态

如果提交时指定 taskId：

```java
String taskId = "query-user-" + userId;

asyncExecutor.submit(
        AsyncTask.of(
                "default",
                "queryUser",
                () -> userService.query(userId)
        )
        .taskId(taskId)
);
```

后续可查询：

```java
TaskExecutionSnapshot snapshot = taskExecutionRegistry.get(taskId);
```

## 9. 验证线程池

看线程名是否符合配置：

```java
asyncExecutor.execute(
        "default",
        "printThread",
        () -> System.out.println(Thread.currentThread().getName())
);
```

输出应类似：

```text
default-async-1
```

如果输出 HTTP 线程名，例如：

```text
http-nio-8080-exec-1
```

可能是触发了 `CALLER_RUNS` 策略，或者任务不是由线程池执行。

## 10. 常见启动问题

### 10.1 找不到线程池

错误类似：

```text
Thread pool not found: biz-query-pool
```

检查配置中是否存在：

```yaml
xjtu:
  iron:
    concurrency:
      thread-pools:
        biz-query-pool:
          ...
```

### 10.2 queueCapacity 配置为 0

有界队列不能配置 0：

```yaml
queue-type: BOUNDED_ARRAY_BLOCKING_QUEUE
queue-capacity: 0 # 错误
```

应该配置大于 0：

```yaml
queue-capacity: 200
```

### 10.3 fallback 线程池配置非法

fallback 线程池只允许：

```text
ABORT
CALLER_RUNS
```

不建议使用 DISCARD、DISCARD_OLDEST、BLOCKING_WAIT。
