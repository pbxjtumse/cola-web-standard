# 16. 消费幂等设计

MessageIdempotencyExecutor 不直接操作数据库，也不指定真实表名。它只生成 namespace、scene、idempotencyKey、shardKey、ownerToken、storeName，然后调用幂等组件暴露的 acquire、markSuccess、markFailed、markDiscarded。

真实写入单表、业务独立表、分表或分库分表，由 idempotent-storage 层根据 scene、storeName 和 shardKey 决定。

落库状态建议：PROCESSING / SUCCESS / FAILED / DISCARDED。ABSENT 是查不到记录的逻辑状态，EXPIRED 是基于 expire_time 或 processing_expire_time 推导出来的逻辑状态，默认不落库。
