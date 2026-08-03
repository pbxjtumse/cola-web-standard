# component-bom 检查清单

本次没有同时提供根 `component-bom` 源码，因此交付包没有擅自覆盖 BOM。

请确认 `component-bom/pom.xml` 已管理本次直接使用的 Foundation 模块：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-core</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-time</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-id</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>foundation-test-support</artifactId>
    <version>${project.version}</version>
</dependency>
```

以及重试组件模块：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>retry-api</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>retry-core</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>retry-config</artifactId>
    <version>${project.version}</version>
</dependency>
```

如果缺少其中任意项，子模块中未写 `<version>` 的依赖会在 Maven 模型解析阶段失败，而不是在 Java 编译阶段失败。
