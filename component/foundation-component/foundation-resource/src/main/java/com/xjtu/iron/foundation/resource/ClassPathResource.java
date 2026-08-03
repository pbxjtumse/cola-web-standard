package com.xjtu.iron.foundation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

/**
 * 读取类路径资源。
 */
public final class ClassPathResource implements Resource {

    /** 去除开头斜杠后的类路径。 */
    private final String path;
    /** 实际查找资源使用的类加载器。 */
    private final ClassLoader classLoader;

    public ClassPathResource(String path) {
        this(path, Thread.currentThread().getContextClassLoader());
    }

    public ClassPathResource(String path, ClassLoader classLoader) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        this.path = path.startsWith("/") ? path.substring(1) : path;
        this.classLoader = classLoader == null ? ClassPathResource.class.getClassLoader() : classLoader;
    }

    @Override
    public String description() {
        return "classpath:" + path;
    }

    @Override
    public InputStream openStream() throws IOException {
        InputStream stream = classLoader.getResourceAsStream(path);
        if (stream == null) {
            throw new ResourceNotFoundException("classpath resource not found: " + path);
        }
        return stream;
    }

    @Override
    public Optional<URI> uri() {
        URL url = classLoader.getResource(path);
        if (url == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(url.toURI());
        } catch (URISyntaxException exception) {
            throw new ResourceNotFoundException("invalid classpath resource URI: " + path, exception);
        }
    }

    @Override
    public boolean exists() {
        return classLoader.getResource(path) != null;
    }
}
