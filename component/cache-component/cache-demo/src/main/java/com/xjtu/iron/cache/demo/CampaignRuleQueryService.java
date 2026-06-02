package com.xjtu.iron.cache.demo;

import com.xjtu.iron.cache.api.CacheClient;
import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.api.model.CacheResult;
import org.springframework.stereotype.Service;

/**
 * 营销活动规则查询服务。
 *
 * <p>这是业务使用缓存组件的典型位置：Service 构造 CacheKey，调用 CacheClient，
 * loader 中访问 Repository。</p>
 */
@Service
public class CampaignRuleQueryService {

    /** 统一缓存访问入口，由 starter 自动装配。 */
    private final CacheClient cacheClient;

    /** 模拟源数据仓储。 */
    private final CampaignRuleRepository campaignRuleRepository;

    /** 创建查询服务。 */
    public CampaignRuleQueryService(CacheClient cacheClient, CampaignRuleRepository campaignRuleRepository) {
        this.cacheClient = cacheClient;
        this.campaignRuleRepository = campaignRuleRepository;
    }

    /** 查询活动规则，只返回业务值。 */
    public CampaignRuleDTO queryRule(Long campaignId) {
        return queryRuleResult(campaignId).getValue();
    }

    /** 查询活动规则，并返回带命中层级和状态的 CacheResult，便于调试。 */
    public CacheResult<CampaignRuleDTO> queryRuleResult(Long campaignId) {
        CacheKey key = buildCampaignRuleKey(campaignId);
        return cacheClient.get(key, CampaignRuleDTO.class, () -> campaignRuleRepository.queryByCampaignId(campaignId));
    }

    /** 删除活动规则缓存。 */
    public void evictRule(Long campaignId) {
        cacheClient.evict(buildCampaignRuleKey(campaignId));
    }

    /** 返回源数据访问次数。 */
    public int getSourceLoadCount() {
        return campaignRuleRepository.getSourceLoadCount();
    }

    /** 构建活动规则缓存 key。 */
    private CacheKey buildCampaignRuleKey(Long campaignId) {
        return CacheKey.of("campaignRule", "marketing", "campaignId:" + campaignId, "v1");
    }
}
