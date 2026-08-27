package com.xjtu.iron.message.core.routing;

import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.spi.ProviderDestination;

/**
 * 逻辑目的地解析器接口。
 *
 * <p>业务侧传入的是 {@code MessageDestination}，其中包含 namespace、name 和可选 providerHint。
 * Resolver 负责把它转换成 Provider 可识别的 {@code ProviderDestination}，也就是 providerName + physicalName。</p>
 */
@FunctionalInterface
public interface DestinationResolver {

    /**
     * 解析逻辑目的地。
     *
     * @param destination 逻辑目的地
     * @return Provider 物理目的地
     */
    ProviderDestination resolve(MessageDestination destination);
}
