# transaction-component V1

`transaction-component` 是本地事务统一执行组件。它封装事务边界、传播行为、隔离级别、超时、rollback-only、生命周期结果和 Spring Transaction Provider；不负责 XA、TCC、Saga 等分布式事务协调。

## 一期模块

```text
transaction-component
├── transaction-api
├── transaction-spi
├── transaction-core
├── transaction-provider-spring
├── transaction-spring-boot-starter
├── transaction-demo-mybatis
└── transaction-demo-jpa
```

## API 分包

```text
api
├── context
├── definition
├── event
├── exception
├── execution
└── status
```

SPI、core、Spring Provider、Starter 和两个 Demo 也都按职责继续拆包，不再把接口、实现、枚举堆在同一个 package。

## MyBatis Demo

一期 MyBatis Demo 使用 XML SQL：

```text
src/main/resources
├── mybatis-config.xml
└── mapper/DemoRecordMapper.xml
```

没有 `@Insert` / `@Select`。

真实运行配置默认使用：

```text
jdbc:mysql://www.xjtu-iron.online:30306/test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
```

账号密码通过环境变量提供：

```bash
export TX_DEMO_MYSQL_USERNAME=root
export TX_DEMO_MYSQL_PASSWORD='your-password'
```

`mvn test` 使用 `application-test.yml` 的 H2 内存库，避免自动测试修改真实 MySQL。

## 核心阅读顺序

1. `transaction-api/.../execution/TransactionExecutor`
2. `transaction-api/.../definition/TransactionOptions`
3. `transaction-core/.../executor/DefaultTransactionExecutor`
4. `transaction-spi/.../provider/TransactionProvider`
5. `transaction-provider-spring/.../transaction/SpringTransactionProvider`
6. `transaction-demo-mybatis/.../service/MybatisTransactionDemoService`
7. `transaction-demo-mybatis/src/main/resources/mapper/DemoRecordMapper.xml`
8. `transaction-demo-jpa/.../service/JpaTransactionDemoService`
9. `docs/flow-v1-detailed.md`

## 一期传播行为

- REQUIRED
- REQUIRES_NEW
- MANDATORY

## 一期不做

- 自定义 `@Transactional`
- AOP 事务语法糖
- 多 TransactionManager 路由
- XA / TCC / Saga
- Reactive Transaction
- NESTED / Savepoint

详细流程见 `docs/flow-v1-detailed.md`。
