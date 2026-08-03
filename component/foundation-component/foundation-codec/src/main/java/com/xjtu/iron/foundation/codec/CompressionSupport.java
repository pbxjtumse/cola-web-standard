package com.xjtu.iron.foundation.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 提供 GZIP 压缩和受限解压能力。
 */
public final class CompressionSupport {

    /** 流式压缩和解压使用的缓冲区大小。 */
    private static final int BUFFER_SIZE = 8192;

    private CompressionSupport() {
    }

    public static byte[] gzip(byte[] value) {
        if (value == null) {
            return null;
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(value);
            gzip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to gzip data", exception);
        }
    }

    /**
     * 解压 GZIP 数据，并限制最大输出长度，避免压缩炸弹消耗全部内存。
     */
    public static byte[] gunzip(byte[] value, int maxOutputBytes) {
        if (value == null) {
            return null;
        }
        if (maxOutputBytes <= 0) {
            throw new IllegalArgumentException("maxOutputBytes must be positive");
        }
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(value));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > maxOutputBytes) {
                    throw new IllegalArgumentException("uncompressed data exceeds limit: " + maxOutputBytes);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to gunzip data", exception);
        }
    }
}
