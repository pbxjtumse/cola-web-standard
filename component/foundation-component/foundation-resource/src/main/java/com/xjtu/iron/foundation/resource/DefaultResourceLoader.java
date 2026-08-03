package com.xjtu.iron.foundation.resource;

import java.nio.file.Path;

/**
 * 默认资源加载器，支持 classpath: 与 file: 前缀。
 */
public final class DefaultResourceLoader implements ResourceLoader {

    @Override
    public Resource load(String location) {
        ResourceLocation resourceLocation = ResourceLocation.of(location);
        if (resourceLocation.isClasspath()) {
            return new ClassPathResource(resourceLocation.value());
        }
        if (resourceLocation.isFile()) {
            return new FileSystemResource(Path.of(resourceLocation.value().substring("file:".length())));
        }
        return new FileSystemResource(Path.of(resourceLocation.value()));
    }
}
