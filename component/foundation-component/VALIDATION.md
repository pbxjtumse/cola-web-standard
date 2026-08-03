# 代码验证说明

## 已完成验证

1. 13 个 Maven POM 均完成 XML 结构解析；
2. 168 个生产 Java 文件使用 `javac --release 17` 完成语法编译；
3. 40 个测试 Java 文件完成语法编译；
4. Jackson、JUnit、ArchUnit 和 Apache Commons 相关源代码使用与真实 API 形状一致的离线桩完成编译检查；
5. 已运行真实 Java 烟雾测试，覆盖 Unicode 截断、命名转换、集合分片、Duration 解析、Deadline、GZIP、Context 编解码、资源读取、Compact UUID 和树循环检测；
6. 烟雾测试结果：`FOUNDATION_SMOKE_OK`；
7. 未发现 `record`、`TODO` 或 `FIXME` 残留。

## 环境限制

当前执行环境没有安装 Maven，并且不能从 Maven Central 下载真实第三方依赖，所以没有在这里执行完整的：

```bash
mvn clean verify
```

因此需要明确区分：

- 纯 Java 17 代码和所有源码的签名级编译已经通过；
- Jackson、Commons、JUnit 和 ArchUnit 的真实运行时兼容性，仍应在你的联网 Maven 环境执行一次完整构建；
- Jackson 泛型、Java Time 和 ObjectMapper 配置测试已经提供，但当前环境没有使用真实 Jackson Jar 运行。

## 本地验证命令

```bash
./scripts/verify.sh
```

或者：

```bash
mvn clean verify
```
