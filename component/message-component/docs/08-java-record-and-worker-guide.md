# 08 V3 record 历史说明

该文件保留 V3 文档入口，防止已有链接失效。

V4 已将消息组件中的全部 record 转换为普通 `final` 不可变类。当前说明请阅读：

- `08-java-class-and-worker-guide.md`
- `09-record-to-class-migration.md`

V3 中关于 Kafka 专用 poll 线程、`AtomicBoolean` 生命周期控制和一个 Worker 对应一个物理 Topic 的结论仍然有效；变化仅发生在不可变数据对象的 Java 表达方式。
