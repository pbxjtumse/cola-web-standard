package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.spi.MessageProvider;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 管理已启用的消息 Provider，并负责按名称选择 Provider。
 */
public final class MessageProviderRegistry implements AutoCloseable {

    /** 按规范化名称保存 Provider。 */
    private final Map<String, MessageProvider> providers;

    /**
     * 创建 Provider 注册表。
     *
     * @param providerCollection 需要注册的 Provider 集合
     */
    public MessageProviderRegistry(Collection<? extends MessageProvider> providerCollection) {
        // Provider 集合不能为空。
        Objects.requireNonNull(providerCollection, "providerCollection must not be null");
        // 使用有序映射，保证关闭顺序和诊断输出稳定。
        Map<String, MessageProvider> actualProviders = new LinkedHashMap<>();
        // 逐个检查并注册 Provider。
        for (MessageProvider provider : providerCollection) {
            // Provider 实例不能为空。
            Objects.requireNonNull(provider, "provider must not be null");
            // 名称执行统一规范化，避免 Kafka 与 kafka 被当作两个 Provider。
            String providerName = normalize(provider.name());
            // Provider 名称不能为空。
            if (providerName.isBlank()) {
                // 空名称会让路由行为不可预测，因此启动阶段立即失败。
                throw new IllegalArgumentException("provider name must not be blank");
            }
            // 同名 Provider 属于配置错误，不允许后注册者静默覆盖。
            MessageProvider previous = actualProviders.putIfAbsent(providerName, provider);
            // 检测重复 Provider。
            if (previous != null) {
                // 明确指出冲突名称，便于启动排障。
                throw new IllegalArgumentException("duplicate provider name: " + providerName);
            }
        }
        // 至少需要一个 Provider 才能构成可用组件。
        if (actualProviders.isEmpty()) {
            // 在构造阶段失败优于首次发送时才暴露问题。
            throw new IllegalArgumentException("at least one message provider is required");
        }
        // 保存不可变注册表。
        this.providers = Map.copyOf(actualProviders);
    }

    /**
     * 根据名称返回 Provider。
     *
     * @param providerName Provider 名称
     * @return 已注册 Provider
     */
    public MessageProvider getRequired(String providerName) {
        // 统一规范化调用方传入名称。
        String normalizedName = normalize(providerName);
        // 查找目标 Provider。
        MessageProvider provider = providers.get(normalizedName);
        // 未找到时立即抛出明确异常。
        if (provider == null) {
            // 将可用 Provider 一并输出，降低排障成本。
            throw new IllegalArgumentException(
                    "message provider not found: " + providerName + ", available=" + providers.keySet());
        }
        // 返回匹配 Provider。
        return provider;
    }

    /**
     * 关闭全部 Provider。
     */
    @Override
    public void close() {
        // 逐个释放 Provider 持有的网络客户端和消费线程。
        for (MessageProvider provider : providers.values()) {
            // 关闭失败不应阻止后续 Provider 继续释放。
            try {
                // 调用 Provider 自己的关闭逻辑。
                provider.close();
            } catch (RuntimeException ignored) {
                // 第一版没有引入日志门面；后续由可观测性集成记录关闭失败。
            }
        }
    }

    /**
     * 规范化 Provider 名称。
     *
     * @param providerName 原始名称
     * @return 小写且去除首尾空格的名称
     */
    private static String normalize(String providerName) {
        // null 被转换为空字符串，随后由调用方做明确校验。
        return providerName == null
                ? ""
                : providerName.trim().toLowerCase(Locale.ROOT);
    }
}
