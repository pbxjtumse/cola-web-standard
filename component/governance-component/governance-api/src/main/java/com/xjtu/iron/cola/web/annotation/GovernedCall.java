package com.xjtu.iron.cola.web.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GovernedCall {

    String name();

    String downstream() default "";

    String operation() default "";

    String fallbackMethod() default "";

    boolean recordArgs() default false;

    boolean recordResult() default false;
}
