package com.xjtu.iron.foundation.resource;

import java.nio.file.Path;

/**
 * 支持 classpath 和本地文件的默认资源加载器。
 */
public final class DefaultResourceLoader implements ResourceLoader {

    /** 加载 classpath 资源使用的类加载器。 */
    private final ClassLoader classLoader;

    public DefaultResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public DefaultResourceLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Resource load(ResourceLocation location) {
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (location.isClasspath()) {
            return new ClassPathResource(location.path(), classLoader);
        }
        return new FileSystemResource(Path.of(location.path()));
    }
}
