package com.xjtu.iron.message.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageListener {

    /** 支持 ${...} 占位符。 */
    String topic();

    /** Pulsar subscription / Kafka group / RocketMQ consumer group。支持 ${...} 占位符。 */
    String subscription() default "";

    String consumerGroup() default "";
}
