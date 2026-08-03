# retry-component 一期最终清理报告

本版本基于用户上传的 `retry-component(2).zip` 进行清理，目标是作为 `component/retry-component` 模块直接导入。

## 已清理的问题

1. 删除 `retry-api` 根包中的旧公共类型，只保留职责分包：
   - `api.execution`
   - `api.policy`
   - `api.backoff`
   - `api.event`
   - `api.exception`

2. 删除 `retry-core` 根包中的旧实现：
   - `DefaultRetryExecutor`
   - `DefaultRetryPolicyRegistry`
   - `UuidRetryIdGenerator`

3. 删除 `retry-config` 根包中的旧自动配置、属性解析和观测类，只保留：
   - `config.autoconfigure`
   - `config.properties`
   - `config.observation`

4. 删除所有交付污染：
   - `target`
   - `.class`
   - `.iml`
   - `__MACOSX`
   - `.DS_Store`

5. 删除重试组件自带 ID 实现，改用冻结后的 Foundation ID：
   - `foundation-id.api.StringIdGenerator`
   - `foundation-id.factory.IdGenerators`
   - `foundation-id.registry.StringIdGeneratorRegistry`

6. 保持 `retry-api` 不依赖 Foundation，避免消息组件只引用重试策略模型时被迫传递基础实现依赖。

7. `retry-core` 复用 Foundation：
   - `foundation-id`：生成 retryId
   - `foundation-core`：参数校验和中断恢复
   - `foundation-time`：墙上时钟接口

8. `retry-config` 负责 Spring 环境下的 retryId 选择：
   - 优先使用名为 `retryIdGenerator` 的 Bean
   - 其次使用 `StringIdGeneratorRegistry` 中名为 `retry` 的生成器
   - 没有 Registry 时默认使用 UUID v7

## 最终模块结构

```text
retry-component
├── retry-api
├── retry-core
├── retry-config
└── retry-demo
```

## 合入前置条件

完整父工程需要已经包含并冻结：

```text
foundation-core
foundation-time
foundation-id
foundation-test-support
```

并在 `component-bom` 中管理以上模块和 retry 模块版本。
