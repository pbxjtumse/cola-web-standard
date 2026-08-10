package com.xjtu.iron.message.core.id;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 基于 foundation-component 统一 ID 能力的消息 ID 生成器适配器。
 *
 * <p>
 * 该类不直接绑定某一个 foundation 具体接口，避免 message-core 因基础组件
 * 内部接口调整而频繁变更。业务工程只需要把 foundation 的 ID 生成方法
 * 以 Supplier 形式传入即可。
 * </p>
 *
 */
public final class FoundationMessageIdGenerator implements MessageIdGenerator {

    /** foundation 侧统一 ID 生成函数。 */
    private final Supplier<String> idSupplier;

    public FoundationMessageIdGenerator(Supplier<String> idSupplier) {
        this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier must not be null");
    }

    /**
     * 创建 foundation ID 适配器。
     *
     * @param idSupplier foundation 侧 ID 生成函数
     * @return 消息 ID 生成器
     */
    public static FoundationMessageIdGenerator from(Supplier<String> idSupplier) {
        return new FoundationMessageIdGenerator(idSupplier);
    }

    @Override
    public String nextId() {
        String id = idSupplier.get();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("foundation id generator returned blank id");
        }
        return id.trim();
    }
}
