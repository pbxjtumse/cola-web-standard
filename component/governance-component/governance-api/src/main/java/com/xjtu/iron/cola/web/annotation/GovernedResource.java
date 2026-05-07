package com.xjtu.iron.cola.web.annotation;


import com.xjtu.iron.cola.web.model.resource.GovernanceResourceType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GovernedResource {

    String name();

    GovernanceResourceType type() default GovernanceResourceType.OUTBOUND;

    String fallbackMethod() default "";

    boolean recordArgs() default false;

    boolean recordResult() default false;
}
