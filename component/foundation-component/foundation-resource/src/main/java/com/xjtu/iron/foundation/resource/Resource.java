package com.xjtu.iron.foundation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * 描述可以读取的二进制资源。
 */
public interface Resource {

    /** 返回用于日志和异常信息的资源说明。 */
    String description();

    /** 打开新的输入流；调用方负责关闭。 */
    InputStream openStream() throws IOException;

    /** 返回资源 URI；内存资源等无法表示时返回空。 */
    default Optional<URI> uri() {
        return Optional.empty();
    }

    /** 判断资源当前是否存在。 */
    boolean exists();
}
