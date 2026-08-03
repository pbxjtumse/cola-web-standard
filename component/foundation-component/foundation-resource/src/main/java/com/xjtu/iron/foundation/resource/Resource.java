package com.xjtu.iron.foundation.resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * 资源抽象，统一 classpath、文件系统和内存资源。
 */
public interface Resource {

    String getLocation();

    boolean exists();

    InputStream openStream() throws IOException;
}
