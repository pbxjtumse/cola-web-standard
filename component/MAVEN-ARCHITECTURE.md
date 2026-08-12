# Component Maven Architecture

## 1. 角色

- `component/pom.xml`：技术组件源码的 **Aggregator + Parent + 内部 dependencyManagement**。
- `xxx-component/pom.xml`：具体技术组件聚合父 POM，只管理本组件特有的第三方版本；不再 import `component-bom`。
- `component-bom/pom.xml`：**对外发布 BOM**，业务工程使用 `type=pom + scope=import` 导入。

## 2. 为什么内部不再 import component-bom

组件源码本身已经通过 parent 链继承 `component/pom.xml`，内部模块版本可直接由 Parent 的 `dependencyManagement` 提供。
如果源码组件再 import `component-bom`，从 `message-component` 等子目录独立读取模型时，Maven 必须先从 Reactor、本地仓库或远程仓库解析 BOM，造成不必要的自依赖。

## 3. 为什么仍保留 component-bom

业务项目通常已经有自己的 Parent（例如 Spring Boot Parent 或公司 Parent），不能为了使用技术组件而改成继承 `component`。
BOM 可以通过 `dependencyManagement` 的 `import` 方式叠加到任何业务工程，因此它是发布边界，而不是源码继承边界。

## 4. 推荐构建方式

同一仓库开发跨组件依赖时，推荐从仓库根目录使用 Reactor：

```bash
mvn -pl component/message-component -am clean compile
mvn -pl component/idempotent-component -am clean compile
```

`-am` 会把目标组件依赖的其它 Reactor 模块一起构建。

如果进入 `component/message-component` 直接执行 `mvn compile`，Parent POM 可以通过 `relativePath` 解析，但跨组件依赖（例如 `retry-api`）仍必须已经存在于本地仓库/私服；这是正常的 Maven artifact 解析规则。

## 5. 外部业务工程消费

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
```

之后业务依赖不再写版本：

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>idempotent-starter</artifactId>
</dependency>
```
