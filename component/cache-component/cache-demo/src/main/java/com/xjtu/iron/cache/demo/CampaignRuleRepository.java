package com.xjtu.iron.cache.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo 源数据仓储。
 *
 * <p>一期调试阶段先用假方法模拟数据库访问，目的是验证缓存组件链路。
 * 缓存组件本身不应该依赖具体数据库。真正接 MySQL/H2 时，只需要把这里改成真实 Mapper/Repository 即可。</p>
 */
@Repository
public class CampaignRuleRepository {

    /** 日志对象，用于观察什么时候真的访问了源数据。 */
    private static final Logger log = LoggerFactory.getLogger(CampaignRuleRepository.class);

    /** 源数据访问次数计数器，用于验证第二次请求是否命中缓存。 */
    private final AtomicInteger sourceLoadCount = new AtomicInteger();

    /**
     * 模拟根据活动 ID 查询数据库。
     *
     * <p>如果缓存生效，第一次请求会打印日志并增加计数；第二次请求应该直接命中 Caffeine，不再进入这里。</p>
     */
    public CampaignRuleDTO queryByCampaignId(Long campaignId) {
        int count = sourceLoadCount.incrementAndGet();
        log.info("[SOURCE] query campaign rule from fake database, campaignId={}, sourceLoadCount={}", campaignId, count);

        CampaignRuleDTO dto = new CampaignRuleDTO();
        dto.setCampaignId(campaignId);
        dto.setRuleName("新人营销规则");
        dto.setRuleContent("满 100 减 20");
        return dto;
    }

    /** 返回当前源数据访问次数。 */
    public int getSourceLoadCount() {
        return sourceLoadCount.get();
    }
}
