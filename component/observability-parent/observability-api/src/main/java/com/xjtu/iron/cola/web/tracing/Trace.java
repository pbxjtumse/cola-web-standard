package com.xjtu.iron.cola.web.tracing;

import java.lang.annotation.*;

/**
 * @author faywong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trace {

    String value() default "";

    boolean recordArgs() default false;
}
