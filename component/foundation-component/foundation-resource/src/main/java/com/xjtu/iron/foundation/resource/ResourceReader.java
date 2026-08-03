package com.xjtu.iron.foundation.resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 以受限方式读取资源内容。
 */
public final class ResourceReader {

    private ResourceReader() {
    }

    /**
     * 读取资源全部字节，并限制最大长度。
     */
    public static byte[] readBytes(Resource resource, int maxBytes) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        try (InputStream input = resource.openStream();
             BoundedInputStream bounded = BoundedInputStream.builder()
                     .setInputStream(input)
                     .setMaxCount((long) maxBytes + 1L)
                     .get()) {
            byte[] content = IOUtils.toByteArray(bounded);
            if (content.length > maxBytes) {
                throw new ResourceLimitExceededException(
                        "resource exceeds limit " + maxBytes + ": " + resource.description()
                );
            }
            return content;
        } catch (IOException exception) {
            throw new ResourceNotFoundException("failed to read resource: " + resource.description(), exception);
        }
    }

    public static String readText(Resource resource, int maxBytes) {
        return readText(resource, maxBytes, StandardCharsets.UTF_8);
    }

    public static String readText(Resource resource, int maxBytes, Charset charset) {
        return new String(readBytes(resource, maxBytes), charset);
    }
}
