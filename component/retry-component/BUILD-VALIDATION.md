# Build Validation

验证日期：2026-08-01

## 已完成的实际验证

### 1. Java 主源码编译

使用 OpenJDK 21 执行：

```text
javac --release 21 -Xlint:all -Werror
```

结果：

- `retry-api` 全部主源码通过。
- `retry-core` 全部主源码通过。
- `retry-config` 在最小 Spring Boot、Spring Framework 与 Micrometer 类型桩下通过静态类型编译。
- `retry-demo` 在同一类型桩下通过静态类型编译。

类型桩编译只能证明 Java 语法、泛型和主要方法调用关系成立，不能替代真实依赖解析和 Spring 容器启动。

### 2. 测试源码编译

使用最小 JUnit Jupiter、Spring Test 与 AssertJ 类型桩，对以下测试源码执行 Java 21 `-Xlint:all -Werror` 编译：

- `RetryPolicyTest`
- `DefaultRetryExecutorTest`
- `DefaultRetryPolicyRegistryTest`
- `RetryAutoConfigurationTest`
- `RetryPolicyPropertiesResolverTest`
- `RetryDemoApplicationTest`

结果：通过。

### 3. 独立核心行为冒烟测试

在不依赖 Maven 和第三方库的情况下，编译并运行独立测试程序，覆盖：

1. 前两次抛出 `IOException`，第三次成功。
2. `RetryDecision.delayOverride` 覆盖默认固定退避。
3. 第一次失败后协作式取消，第二次操作不执行。
4. 调用方自定义 `retryId` 被保留。
5. 重复注册同名策略被拒绝。
6. `IOException` 规则优先于较宽泛的 `Exception` 规则。
7. 分类器把异常判为成功时，执行器返回 `EXECUTION_FAILED`。

运行结果：

```text
RETRY_SMOKE_OK
```

### 4. 注释风格检查

执行：

```bash
python scripts/verify-comment-style.py
```

结果：

```text
Java comment style verification passed: 48 files
```

检查范围包括主源码和测试源码，主要规则为：

- package/import 行没有注释；
- 不允许代码后的机械行尾注释；
- 不允许上一版本生成的通用机械短语；
- 构造器、Getter/Setter 和 Builder 简单赋值方法前不保留重复 Javadoc。

### 5. 代码逻辑一致性检查

将 1.2.0 与 1.3.0 的全部 Java 文件去除块注释、行注释和空白差异后逐文件比较。

结果：

```text
JAVA_LOGIC_MATCH
```

说明本版本没有修改重试执行逻辑，只调整注释、文档、校验脚本和版本号。

### 6. POM、YAML 与源树检查

- 所有父子 POM 均通过 XML 解析。
- Demo YAML 通过解析。
- 主源码和测试源码树中不存在 `.class`、`target`、`.pyc` 或 `__pycache__` 产物。
- 父 POM 版本为 `1.3.0-SNAPSHOT`。
- 根项目名称仍为 `retry-component`。
- 模块名称仍为 `retry-api`、`retry-core`、`retry-config`、`retry-demo`。
- Java 包名仍为 `com.xjtu.iron.retry`。

## 当前环境限制

当前容器没有 Maven，也不能完成真实 Maven Central 依赖解析，因此没有执行：

```bash
mvn clean verify
```

也没有声称 Spring Boot 自动配置测试和真实 Micrometer 指标测试已经运行。

## 合并到正式仓库后的必做验证

```bash
python scripts/verify-comment-style.py
mvn --version
mvn clean verify
mvn -pl retry-demo -am spring-boot:run
```

随后调用：

```bash
curl 'http://localhost:18090/demo/retry/exception?failures=2'
curl 'http://localhost:18090/demo/retry/result?pendingTimes=2'
curl 'http://localhost:18090/demo/retry/server-delay'
curl 'http://localhost:18090/demo/retry/cancel'
curl 'http://localhost:18090/demo/retry/non-retryable'
```

并检查：

- Spring 配置绑定；
- 策略继承和显式列表清空；
- 自定义 Bean 覆盖；
- ApplicationEvent 发布；
- Actuator/Micrometer 指标；
- 全部 JUnit 测试。
