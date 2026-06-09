

1.CompletableFuture<Void> 如何感知完成和异常？会不会让业务代码臃肿
```java


try {
    future.join();
    log.info("任务成功");
} catch (CompletionException ex) {
    log.error("任务失败", ex.getCause());
}
```
2. whenComplete()：成功失败都回调 
```java
future.whenComplete((unused, error) -> {
        if (error == null) {
        log.info("任务成功");
        } else {
            log.error("任务失败", error);
        }
});

```
3.handle()：成功失败都处理，并返回新值
```java
CompletableFuture<String> result = future.handle((unused, error) -> {
    if (error == null) {
        return "SUCCESS";
    }
    return "FAILED";
}); 
```
4. future.isDone();
   future.isCompletedExceptionally();
   future.isCancelled();