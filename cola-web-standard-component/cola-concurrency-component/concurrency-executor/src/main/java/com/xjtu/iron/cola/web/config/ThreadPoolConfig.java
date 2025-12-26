package com.xjtu.iron.cola.web.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@Setter
@Getter
@Builder
public class ThreadPoolConfig {
    private String name;
    private int coreSize;
    private int maxSize;
    private long keepAliveSeconds;
    private int queueSize;
    private boolean enableTtl;
    private String rejectPolicy;
    private Set<String> tags = new HashSet<>();

}

