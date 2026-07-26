# 构建与验证报告

- 生成日期：2026-07-26
- Java 版本基线：17
- Java 文件数：54
- Java 总行数：6055
- 注释行数（近似统计）：2368
- POM XML 格式检查：通过
- 公共模块 `javac --release 17 -Xlint:all -Werror`：通过
- 内存发送消费闭环：通过
- 消息上下文契约验证：通过
- 外部 Provider Java 源码语法扫描：未发现语法错误
- 外部 SDK 完整 Maven 编译：当前环境无 Maven 且无法下载外部制品，未执行
- 真实 Kafka/RocketMQ/Pulsar 集群集成测试：未执行

## 已实际验证的公共契约

- 根消息 correlationId 默认等于 messageId；
- source 在未配置时保持缺失；
- 子消息继承 correlationId；
- 子消息 causationId 指向直接父消息；
- STRICT 路由缺失返回 REJECTED + ROUTING_ERROR；
- 业务系统头伪造被拒绝；
- 入站逻辑目的地不匹配被拒绝。
