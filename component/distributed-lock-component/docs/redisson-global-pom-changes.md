# v26：完整工程 POM 需要同步的两处治理修改

本 `distributed-lock-component` 已恢复统一的 BOM 继承关系，但完整 `cola-web-standard` 工程还有两处文件不在本压缩包中，必须同步修改。

## 1. `cola-web-standard/pom.xml`：只管理 Redisson 第三方版本

项目既有规则：第三方版本由最外层根 POM 统一治理，具体子模块不写版本。

```xml
<properties>
    <redisson.version>4.7.0</redisson.version>
</properties>
```

在根 `<dependencyManagement><dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>${redisson.version}</version>
</dependency>
```

不要把 Redisson 放进根 `<dependencies>`；根 POM 只治理版本，真正使用 Redisson 的模块是 `distributed-lock-provider-redisson`。

## 2. `component/component-bom/pom.xml`：增加新的自研 artifact

`component-bom` 负责 `com.xjtu.iron` 自研 artifact 的版本。新增 Provider 后至少要加入：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-provider-redisson</artifactId>
    <version>${project.version}</version>
</dependency>
```

如果 BOM 中尚未完整登记分布式锁模块，建议统一保持：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-api</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-core</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-provider-redis</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-provider-redisson</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-fencing-provider-jdbc</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 3. 本组件内部的最终 POM 继承链

```text
cola-web-standard root
  └─ 管第三方版本：Redisson / Spring Boot / Testcontainers ...

component-bom
  └─ 管 com.xjtu.iron 自研 artifact 版本

distributed-lock-component/pom.xml
  ├─ import component-bom
  └─ modules
      ├─ distributed-lock-api
      ├─ distributed-lock-core
      ├─ distributed-lock-provider       <- Provider 聚合器
      ├─ distributed-lock-starter
      └─ distributed-lock-demo

distributed-lock-provider/pom.xml
  └─ modules
      ├─ distributed-lock-provider-redis
      ├─ distributed-lock-provider-redisson
      └─ distributed-lock-fencing-provider-jdbc
```

根 `distributed-lock-component` 不再越级直接聚合 `distributed-lock-provider-redisson`。

## 4. 子模块依赖规则

子模块只声明真实依赖，不写内部版本：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>distributed-lock-core</artifactId>
</dependency>

<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
</dependency>
```

`component-bom` / 根 dependencyManagement 只负责版本解析，不会自动把这些依赖加入 classpath。
