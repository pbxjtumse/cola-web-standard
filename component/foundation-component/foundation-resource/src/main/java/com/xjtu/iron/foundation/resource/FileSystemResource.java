package com.xjtu.iron.foundation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 读取本地文件系统资源。
 */
public final class FileSystemResource implements Resource {

    /** 标准化后的本地文件路径。 */
    private final Path path;

    public FileSystemResource(Path path) {
        this.path = java.util.Objects.requireNonNull(path, "path must not be null").normalize();
    }

    @Override
    public String description() {
        return "file:" + path;
    }

    @Override
    public InputStream openStream() throws IOException {
        if (!exists()) {
            throw new ResourceNotFoundException("file resource not found: " + path);
        }
        return Files.newInputStream(path);
    }

    @Override
    public Optional<URI> uri() {
        return Optional.of(path.toUri());
    }

    @Override
    public boolean exists() {
        return Files.isRegularFile(path);
    }

    public Path getPath() {
        return path;
    }
}
