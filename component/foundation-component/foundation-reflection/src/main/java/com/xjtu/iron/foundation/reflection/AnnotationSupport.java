package com.xjtu.iron.foundation.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * 提供注解查找能力。
 */
public final class AnnotationSupport {

    private AnnotationSupport() {
    }

    public static <A extends Annotation> Optional<A> find(AnnotatedElement element, Class<A> annotationType) {
        if (element == null || annotationType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(element.getAnnotation(annotationType));
    }

    public static boolean isPresent(AnnotatedElement element, Class<? extends Annotation> annotationType) {
        return element != null && annotationType != null && element.isAnnotationPresent(annotationType);
    }
}
