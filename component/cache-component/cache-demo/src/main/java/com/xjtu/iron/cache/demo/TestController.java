package com.xjtu.iron.cache.demo;

import com.xjtu.iron.cache.api.CacheKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/{campaignId}")
    public CampaignRuleDTO query(@PathVariable Long campaignId) {
        return campaignRuleQueryService.queryRule(campaignId);
    }
}
