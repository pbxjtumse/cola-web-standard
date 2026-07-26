package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.MessageCategory;
import com.xjtu.iron.message.api.MessageDestination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 保存逻辑目的地到多个 Provider 的精确路由。
 */
public final class DestinationRouteRegistry {

    /** 按逻辑目的地和 Provider 保存路由。 */
    private final Map<RouteKey, Map<String, DestinationRoute>> routes;

    /**
     * 创建路由注册表。
     *
     * @param routeCollection 路由集合
     */
    public DestinationRouteRegistry(Collection<DestinationRoute> routeCollection) {
        // 使用有序 Map 保证配置冲突诊断稳定。
        Map<RouteKey, Map<String, DestinationRoute>> mutableRoutes = new LinkedHashMap<>();
        // null 视为空路由集合。
        Collection<DestinationRoute> actualRoutes = routeCollection == null
                ? List.of()
                : routeCollection;
        // 逐条注册路由。
        for (DestinationRoute route : actualRoutes) {
            // 路由不能为空。
            if (route == null) {
                // 空路由属于启动配置错误。
                throw new IllegalArgumentException("destination route must not be null");
            }
            // 创建不包含 providerHint 的逻辑路由键。
            RouteKey key = new RouteKey(route.namespace(), route.name(), route.category());
            // 获取同一逻辑目的地下的 Provider 路由表。
            Map<String, DestinationRoute> providerRoutes = mutableRoutes.computeIfAbsent(
                    key,
                    ignored -> new LinkedHashMap<>());
            // 同一逻辑目的地和 Provider 不能出现两条路由。
            DestinationRoute previous = providerRoutes.putIfAbsent(route.providerName(), route);
            // 冲突时立即终止启动。
            if (previous != null) {
                // 输出逻辑键和 Provider，便于定位配置错误。
                throw new IllegalArgumentException(
                        "duplicate destination route: " + key + ", provider=" + route.providerName());
            }
        }
        // 深度复制为不可变结构。
        Map<RouteKey, Map<String, DestinationRoute>> immutableRoutes = new LinkedHashMap<>();
        // 逐个复制内部 Provider Map。
        mutableRoutes.forEach((key, value) -> immutableRoutes.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(value))));
        // 保存顶层不可变 Map。
        this.routes = Collections.unmodifiableMap(new LinkedHashMap<>(immutableRoutes));
    }

    /**
     * 创建空路由注册表。
     *
     * @return 空注册表
     */
    public static DestinationRouteRegistry empty() {
        // 空路由场景使用默认物理名称策略。
        return new DestinationRouteRegistry(List.of());
    }

    /**
     * 查找指定 Provider 的精确路由。
     *
     * @param destination 逻辑目的地
     * @param providerName Provider 名称
     * @return 路由
     */
    public Optional<DestinationRoute> find(
            MessageDestination destination,
            String providerName) {
        // 构建逻辑路由键。
        RouteKey key = RouteKey.of(destination);
        // 查找逻辑目的地全部 Provider 路由。
        Map<String, DestinationRoute> providerRoutes = routes.get(key);
        // 没有任何路由时返回空。
        if (providerRoutes == null) {
            // 返回 Optional.empty。
            return Optional.empty();
        }
        // 返回指定 Provider 路由。
        // Provider 名称在公共模型中统一为小写稳定标识。
        String normalizedProvider = providerName == null
                ? null
                : providerName.trim().toLowerCase(Locale.ROOT);
        // 返回指定 Provider 路由。
        return Optional.ofNullable(providerRoutes.get(normalizedProvider));
    }

    /**
     * 返回逻辑目的地配置的全部路由。
     *
     * @param destination 逻辑目的地
     * @return 不可变路由列表
     */
    public List<DestinationRoute> findAll(MessageDestination destination) {
        // 查找内部 Provider Map。
        Map<String, DestinationRoute> providerRoutes = routes.get(RouteKey.of(destination));
        // 没有路由时返回空列表。
        if (providerRoutes == null || providerRoutes.isEmpty()) {
            // 返回 JDK 空不可变列表。
            return List.of();
        }
        // 复制为稳定顺序列表。
        return Collections.unmodifiableList(new ArrayList<>(providerRoutes.values()));
    }

    /**
     * 路由表内部逻辑身份。
     *
     * @param namespace 命名空间
     * @param name 名称
     * @param category 类别
     */
    private record RouteKey(String namespace, String name, MessageCategory category) {

        /**
         * 从逻辑目的地创建路由键。
         */
        private static RouteKey of(MessageDestination destination) {
            // providerHint 不属于逻辑身份。
            return new RouteKey(
                    destination.namespace(),
                    destination.name(),
                    destination.category());
        }
    }
}
