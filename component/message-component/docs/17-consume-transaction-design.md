# 17. 消费事务设计

事务模板只包住业务 Handler 与幂等终态更新，不包住 acquire，也不包住 Provider ack / commit。

标准顺序：acquire PROCESSING -> transaction begin -> handler -> markSuccess / markDiscarded -> transaction commit -> Provider ack / commit。

Handler 抛异常或返回 RETRY 时，业务事务回滚，markFailed 在事务外尽力记录，最终返回 RETRY。
