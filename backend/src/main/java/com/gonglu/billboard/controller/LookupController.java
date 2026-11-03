package com.gonglu.billboard.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.entity.Section;
import com.gonglu.billboard.service.BillboardService;
import com.gonglu.billboard.service.CacheService;
import com.gonglu.billboard.service.ContractService;
import com.gonglu.billboard.service.MerchantService;
import com.gonglu.billboard.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "下拉联动数据（登录即可读）")
@RestController
@RequestMapping("/api/lookup")
public class LookupController {

    private final SectionService sectionService;
    private final BillboardService billboardService;
    private final MerchantService merchantService;
    private final ContractService contractService;
    private final CacheService cacheService;

    public LookupController(SectionService sectionService, BillboardService billboardService,
                            MerchantService merchantService, ContractService contractService,
                            CacheService cacheService) {
        this.sectionService = sectionService;
        this.billboardService = billboardService;
        this.merchantService = merchantService;
        this.contractService = contractService;
        this.cacheService = cacheService;
    }

    @Operation(summary = "表单下拉选项：路段/广告牌/商家/合同")
    @GetMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> lookup() {
        // 热点数据走 Redis 缓存（5 分钟）；业务写操作会主动清除；Redis 不可用时直连数据库
        Map<String, Object> cached = cacheService.get(CacheService.KEY_LOOKUP);
        if (cached != null) {
            return R.ok(cached);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sections", sectionService.list(Wrappers.<Section>lambdaQuery().orderByAsc(Section::getId)));
        result.put("billboards", billboardService.list(Wrappers.<Billboard>lambdaQuery()
                .select(Billboard::getId, Billboard::getCode, Billboard::getSectionId, Billboard::getSide,
                        Billboard::getStakeActual, Billboard::getStatus)
                .orderByAsc(Billboard::getId)));
        result.put("merchants", merchantService.list(Wrappers.<Merchant>lambdaQuery()
                .select(Merchant::getId, Merchant::getName, Merchant::getBlacklisted)
                .orderByAsc(Merchant::getId)));
        result.put("contracts", contractService.list(Wrappers.<Contract>lambdaQuery()
                .select(Contract::getId, Contract::getCode, Contract::getStatus, Contract::getMerchantId)
                .orderByDesc(Contract::getId)));
        cacheService.set(CacheService.KEY_LOOKUP, result);
        return R.ok(result);
    }
}
