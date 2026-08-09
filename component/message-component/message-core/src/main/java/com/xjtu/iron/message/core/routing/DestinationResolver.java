package com.xjtu.iron.message.core.routing;

import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.spi.ProviderDestination;

/**
 * 定义逻辑目的地到 Provider 物理目的地的解析契约。
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
