package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.mapper.ReceivableMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计指标
 */
@Service
public class StatsService {

    private final BillboardService billboardService;
    private final MerchantService merchantService;
    private final ContractService contractService;
    private final SectionService sectionService;
    private final UserService userService;
    private final ReceivableMapper receivableMapper;

    public StatsService(BillboardService billboardService, MerchantService merchantService,
                        ContractService contractService, SectionService sectionService,
                        UserService userService, ReceivableMapper receivableMapper) {
        this.billboardService = billboardService;
        this.merchantService = merchantService;
        this.contractService = contractService;
        this.sectionService = sectionService;
        this.userService = userService;
        this.receivableMapper = receivableMapper;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("billboardCount", billboardService.count());
        result.put("rentedCount", billboardService.count(Wrappers.<Billboard>lambdaQuery().eq(Billboard::getStatus, "在租")));
        result.put("idleCount", billboardService.count(Wrappers.<Billboard>lambdaQuery().eq(Billboard::getStatus, "空闲")));
        result.put("merchantCount", merchantService.count());
        result.put("blacklistCount", merchantService.count(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getBlacklisted, true)));
        result.put("activeContractCount", contractService.count(Wrappers.<Contract>lambdaQuery().eq(Contract::getStatus, "生效")));
        result.put("draftContractCount", contractService.count(Wrappers.<Contract>lambdaQuery().eq(Contract::getStatus, "草稿")));
        result.put("sectionCount", sectionService.count());
        result.put("userCount", userService.count());

        Map<String, Object> sums = receivableMapper.sumAll();
        result.put("totalReceivable", sums.get("totalAmount"));
        result.put("totalReceived", sums.get("totalReceived"));
        result.put("totalArrears", sums.get("totalArrears"));

        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(90);

        // 90 天内到期的生效合同
        List<Map<String, Object>> expiringContracts = new ArrayList<>();
        for (Contract c : contractService.list(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getStatus, "生效")
                .ge(Contract::getEndDate, today)
                .le(Contract::getEndDate, deadline))) {
            Merchant m = merchantService.getById(c.getMerchantId());
            Map<String, Object> row = new HashMap<>();
            row.put("code", c.getCode());
            row.put("merchantName", m != null ? m.getName() : "-");
            row.put("endDate", c.getEndDate());
            expiringContracts.add(row);
        }
        result.put("expiringContracts", expiringContracts);

        // 90 天内证件到期的广告牌（路政许可/保险）
        List<Map<String, Object>> expiringCompliance = new ArrayList<>();
        for (Billboard b : billboardService.list()) {
            addCompliance(expiringCompliance, b, "路政许可", b.getPermitExpire(), today, deadline);
            addCompliance(expiringCompliance, b, "保险", b.getInsuranceExpire(), today, deadline);
            addCompliance(expiringCompliance, b, "年审", b.getAnnualCheckExpire(), today, deadline);
        }
        result.put("expiringCompliance", expiringCompliance);
        return result;
    }

    private void addCompliance(List<Map<String, Object>> list, Billboard b, String type,
                               LocalDate expire, LocalDate today, LocalDate deadline) {
        if (expire != null && !expire.isBefore(today) && !expire.isAfter(deadline)) {
            Map<String, Object> row = new HashMap<>();
            row.put("code", b.getCode());
            row.put("type", type);
            row.put("expireDate", expire);
            list.add(row);
        }
    }
}
