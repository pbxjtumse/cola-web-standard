# Distributed Lock POM 治理规则

## 职责边界

| 层级 | 职责 | 不应该做什么 |
|---|---|---|
| `cola-web-standard/pom.xml` | 第三方版本、BOM、插件统一治理 | 不因某组件使用 Redisson 就把 Redisson 放进全局 dependencies |
| `component-bom` | `com.xjtu.iron` 自研 artifact 版本治理 | 不聚合 module，不自动引入 jar |
| `distributed-lock-component` | 导入 `component-bom` + 聚合一级模块 | 不越级聚合具体 Provider |
| `distributed-lock-provider` | 聚合所有锁/发号 Provider | 不维护第三方版本 |
| 具体 jar module | 声明真实依赖 | 不重复写版本 |

## 当前层次

```text
distributed-lock-component
├── distributed-lock-api
├── distributed-lock-core
├── distributed-lock-provider
│   ├── distributed-lock-provider-redis
│   ├── distributed-lock-provider-redisson
│   └── distributed-lock-fencing-provider-jdbc
├── distributed-lock-starter
└── distributed-lock-demo
```

## 为什么 Provider 必须放在 provider aggregator 下

这样未来加入 ZooKeeper / Etcd 时只修改 `distributed-lock-provider/pom.xml`：

```xml
<modules>
    <module>distributed-lock-provider-redis</module>
    <module>distributed-lock-provider-redisson</module>
    <module>distributed-lock-provider-zookeeper</module>
    <module>distributed-lock-provider-etcd</module>
    <module>distributed-lock-fencing-provider-jdbc</module>
</modules>
```

根组件结构不会因为 Provider 数量增加而越来越乱。
