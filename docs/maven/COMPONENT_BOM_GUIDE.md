# 为什么需要 component-bom

## 1. BOM 解决的不是“引入依赖”，而是“统一版本”

`component-bom` 的 packaging 是 `pom`，只包含 `dependencyManagement`，没有普通 `dependencies`。

导入 BOM 不会自动引入 Cache、Message、Retry 等组件。只有业务工程真正声明某个组件依赖时，该组件才进入 classpath。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.xjtu.iron</groupId>
            <artifactId>component-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.xjtu.iron</groupId>
        <artifactId>cache-starter</artifactId>
    </dependency>
</dependencies>
```

上面的配置只引入 `cache-starter`。BOM 只是让它不再需要单独写版本。

## 2. 父 POM 和 BOM 的职责不同

| 能力 | 父 POM | component-bom |
|---|---|---|
| Java 17 | 是 | 否 |
| Compiler/Surefire/Enforcer | 是 | 否 |
| Spring Boot、COLA 等第三方版本 | 是 | 否 |
| 自研 Cache/Message/Retry 等组件版本 | 不再手工枚举 | 是 |
| 外部项目不继承父 POM时能否使用 | 不能继承构建规范 | 可以导入版本清单 |
| 是否自动引入实际依赖 | 父 POM普通 dependencies 会继承 | 不会 |

父 POM面向“如何构建”；BOM 面向“各组件使用哪个版本”。

## 3. 为什么不继续把 component 包全部写在根 POM

旧根 POM手工管理了大量 `component` 产物，存在以下问题：

1. 聚合 POM、Demo、空模块也被错误地加入版本管理；
2. Foundation、Message、Retry 新增后，根清单没有同步；
3. 根 POM每增加一个组件就持续膨胀；
4. 外部业务项目通常不会继承 `cola-web-standard-parent`；
5. 组件版本清单和整个工程的构建规则混在了一起。

本版本将职责调整为：

```text
cola-web-standard-parent
├── 第三方 BOM
├── Java 与 Maven 插件规范
└── 顶层 COLA 应用模块版本

component-bom
└── 53 个可消费技术组件的统一版本
```

根 POM中不再出现 `cache-api`、`message-core`、`retry-core` 等具体组件包。

## 4. 为什么组件内部也要导入 BOM

组件内部同样存在跨模块依赖：

```text
message-core -> message-api
cache-starter -> cache-core
retry-core -> retry-api
```

组件根 POM导入 `component-bom` 后，子模块能够统一省略内部版本：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>retry-api</artifactId>
</dependency>
```

不再出现：

```xml
<version>1.0.0-SNAPSHOT</version>
```

## 5. BOM 中包含什么

当前 BOM 只包含具备实际源码或资源的可消费 Jar：

- API；
- Core；
- Config；
- Provider；
- Integration；
- Starter；
- 可复用 Test Support/Testkit。

不包含：

- `*-component` 聚合 POM；
- `*-provider` 等目录聚合 POM；
- Demo；
- Architecture Tests；
- 当前完全空白的占位 Jar。
