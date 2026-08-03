package com.xjtu.iron.retry.config.observation;

import com.xjtu.iron.retry.api.event.RetryEvent;
import com.xjtu.iron.retry.api.event.RetryListener;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Objects;

/** 将核心 RetryEvent 原样桥接为 Spring ApplicationEvent。 */
public final class SpringApplicationRetryListener implements RetryListener {

    /** Spring 事件发布器。 */
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringApplicationRetryListener(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = Objects.requireNonNull(
                applicationEventPublisher,
                "applicationEventPublisher must not be null"
        );
    }

    /** 将核心事件发布到 Spring 应用上下文。 */
    @Override
    public void onEvent(RetryEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
