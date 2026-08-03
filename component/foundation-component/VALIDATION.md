# 代码验证说明

## 已完成验证

1. `foundation-time` 使用 `javac --release 17 -Xlint:all -Werror` 编译通过；
2. `foundation-id` 使用同样参数编译通过；
3. `retry-api`、`retry-core` 与修改后的 `foundation-id` 联合编译通过；
4. UUID v7、ULID、Nano ID 和 Snowflake 各生成 10000 个 ID 的真实烟雾测试通过；
5. UUID v7 版本位、唯一性和单实例单调性检查通过；
6. ULID 长度、唯一性和字典序单调性检查通过；
7. Snowflake 唯一性、递增性和 worker 位检查通过；
8. 重试组件第三次成功并使用 Foundation UUID v7 生成 retryId 的集成测试通过；
9. ID 包结构、重复 API、构建目录和 IDE 文件检查通过；
10. 烟雾测试结果：`FOUNDATION_RETRY_INTEGRATION_OK`。

## 环境限制

当前执行环境没有 Maven，不能从 Maven Central 解析真实 JUnit、ArchUnit、Spring Boot、Jackson 和 Apache Commons 依赖，因此没有声称执行完整的：

```bash
mvn clean verify
```

合入完整工程后必须执行：

```bash
mvn -pl foundation-component -am clean verify
mvn -pl retry-component -am clean verify
```
