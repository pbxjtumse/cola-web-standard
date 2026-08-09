# Spring Boot 配置

## 一、顶层开关

```yaml
xjtu:
  iron:
    retry:
      enabled: true
      publish-spring-events: true
      metrics-enabled: true
```

- `enabled=false`：不装配重试自动配置。
- `publish-spring-events=false`：不创建核心事件到 Spring `ApplicationEvent` 的桥接监听器。
- `metrics-enabled=false`：即使类路径存在 Micrometer，也不创建指标监听器。

## 二、完整策略示例

```yaml
xjtu:
  iron:
    retry:
      policies:
        remote-base:
          max-attempts: 3
          max-duration: 5s
          operation-safety: READ_ONLY
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 16
          retry-failure-category: TRANSIENT
          retry-failure-code: REMOTE_TRANSIENT_FAILURE
          retry-on:
            - java.io.IOException
            - java.util.concurrent.TimeoutException
          stop-on:
            - java.lang.IllegalArgumentException
          abort-on:
            - java.lang.SecurityException
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0

        payment-query:
          base-policy: remote-base
          max-attempts: 5
          max-duration: 20s

        message-send:
          max-attempts: 3
          max-duration: 5s
          operation-safety: IDEMPOTENCY_PROTECTED
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 8
          retry-failure-category: TRANSIENT
          retry-failure-code: MESSAGE_SEND_RETRYABLE_FAILURE
          retry-on:
            - java.io.IOException
            - java.util.concurrent.TimeoutException
          stop-on:
            - java.lang.IllegalArgumentException
          abort-on:
            - java.lang.SecurityException
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

`message-send` 是给消息组件二期可靠发送预留的短时同步重试策略。消息是否已经进入 Broker、发送结果是否为 `UNKNOWN`，由消息组件根据 Provider 结果自行判断，retry-component 不处理 MQ 语义。

## 三、字段语义

| 字段 | 含义 | 默认值 |
|---|---|---|
| `base-policy` | 父策略名 | 无 |
| `max-attempts` | 总尝试次数，包含第一次 | 3 |
| `max-duration` | 整个逻辑执行的最大时长 | 30s |
| `operation-safety` | READ_ONLY/IDEMPOTENT/IDEMPOTENCY_PROTECTED/NON_IDEMPOTENT/UNSPECIFIED | UNSPECIFIED |
| `safety-mode` | ALLOW/WARN/REJECT | WARN |
| `traverse-causes` | 是否检查异常 cause | false |
| `max-cause-depth` | 最多检查多少层异常对象 | 16 |
| `retry-failure-category` | YAML retry-on 规则共用失败分类 | TRANSIENT |
| `retry-failure-code` | YAML retry-on 规则共用稳定错误码 | CONFIGURED_RETRYABLE |
| `retry-on` | 可重试异常类名列表 | 空 |
| `stop-on` | 正常停止异常类名列表 | 空 |
| `abort-on` | 立即中止异常类名列表 | 空 |

## 四、继承三态规则

标量字段使用包装类型，只有子策略显式配置时才覆盖父策略。

列表字段存在三种状态：

1. `null`：子策略没有配置，继承父策略。
2. `[]`：子策略显式清空父策略列表。
3. 非空列表：子策略整体替换父策略列表。

示例：

```yaml
xjtu:
  iron:
    retry:
      policies:
        base:
          retry-on:
            - java.io.IOException

        inherit:
          base-policy: base

        clear:
          base-policy: base
          retry-on: []

        replace:
          base-policy: base
          retry-on:
            - java.util.concurrent.TimeoutException
```

解析结果：

```text
inherit -> [IOException]
clear   -> []
replace -> [TimeoutException]
```

列表不做隐式合并，避免父子策略重复或冲突规则难以判断。

## 五、循环与缺失父策略

以下配置会在应用启动阶段失败：

```yaml
a:
  base-policy: b
b:
  base-policy: c
c:
  base-policy: a
```

错误信息会包含完整路径。父策略不存在同样会立即失败，不会降级成默认策略。

## 六、退避配置

### NONE

```yaml
backoff:
  type: NONE
```

### FIXED

```yaml
backoff:
  type: FIXED
  delay: 500ms
```

### EXPONENTIAL

```yaml
backoff:
  type: EXPONENTIAL
  initial-delay: 100ms
  max-delay: 2s
  multiplier: 2.0
```

### EXPONENTIAL_FULL_JITTER

```yaml
backoff:
  type: EXPONENTIAL_FULL_JITTER
  initial-delay: 100ms
  max-delay: 2s
  multiplier: 2.0
```

按失败类别选择不同策略、服务端 `Retry-After` 等高级规则应使用代码构建，避免 YAML 变成复杂规则语言。

## 七、异常类加载

配置模块使用线程上下文类加载器加载异常类，适配 Spring Boot、应用服务器和插件式运行环境。若类不存在或不是 `Throwable` 子类，应用启动失败。

## 八、可覆盖 Bean

调用方可以提供：

- `RetryPolicyRegistry`
- `RetryClock`
- `RetrySleeper`
- `foundation-id` 的 `StringIdGenerator`
- `RetryListener`
- `RetryExecutor`

自动配置使用缺失 Bean 条件，不会强行替换应用自定义实现。
