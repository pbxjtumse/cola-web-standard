package com.xjtu.iron.distributed.lock.demo;

import com.xjtu.iron.distributed.lock.core.event.LockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Demo 侧事件监听示例。 */
@Component
public class DistributedLockEventLogger {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockEventLogger.class);

    @EventListener
    public void onLockEvent(LockEvent event) {
        log.info("distributed lock event type={}, stage={}, status={}, provider={}, lockName={}",
                event.eventType(), event.stage(), event.stage(), event.providerName(), event.lockName());
    }
}
