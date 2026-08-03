package com.xjtu.iron.foundation.test.resource;

import com.xjtu.iron.foundation.resource.ByteArrayResource;
import com.xjtu.iron.foundation.resource.Resource;

import java.nio.charset.StandardCharsets;

/**
 * 创建测试资源。
 */
public final class ResourceFixtures {

    private ResourceFixtures() {
    }

    public static Resource utf8(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8), "test UTF-8 resource");
    }
}
