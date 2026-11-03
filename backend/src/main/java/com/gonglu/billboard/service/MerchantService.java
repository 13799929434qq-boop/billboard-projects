package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.entity.Receivable;
import com.gonglu.billboard.mapper.MerchantMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MerchantService extends ServiceImpl<MerchantMapper, Merchant> {

    private final ContractService contractService;
    private final ReceivableService receivableService;
    private final LogService logService;
    private final CacheService cacheService;

    public MerchantService(@Lazy ContractService contractService, @Lazy ReceivableService receivableService,
                           LogService logService, CacheService cacheService) {
        this.contractService = contractService;
        this.receivableService = receivableService;
        this.logService = logService;
        this.cacheService = cacheService;
    }

    public List<Merchant> listAll(String keyword, Boolean blacklisted) {
        List<Merchant> list = list(Wrappers.<Merchant>lambdaQuery()
                .and(StringUtils.hasText(keyword), q -> q.like(Merchant::getName, keyword)
                        .or().like(Merchant::getContact, keyword)
                        .or().like(Merchant::getPhone, keyword))
                .eq(blacklisted != null, Merchant::getBlacklisted, blacklisted)
                .orderByAsc(Merchant::getId));
        fillArrears(list);
        return list;
    }

    /** 填充每个商家的欠款合计 */
    public void fillArrears(List<Merchant> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Receivable> receivables = receivableService.list();
        Map<Long, BigDecimal> arrearsMap = receivables.stream()
                .collect(Collectors.groupingBy(Receivable::getMerchantId,
                        Collectors.mapping(r -> r.getAmount().subtract(r.getReceived()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        for (Merchant m : list) {
            m.setArrears(arrearsMap.getOrDefault(m.getId(), BigDecimal.ZERO));
        }
    }

    public Merchant create(Merchant merchant) {
        if (!StringUtils.hasText(merchant.getName())) {
            throw new BizException("商家名称不能为空");
        }
        Long exists = count(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getName, merchant.getName()));
        if (exists > 0) {
            throw new BizException("商家 " + merchant.getName() + " 已存在");
        }
        if (merchant.getBlacklisted() == null) {
            merchant.setBlacklisted(false);
        }
        save(merchant);
        logService.add("新增", "新增商家 " + merchant.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return merchant;
    }

    public Merchant update(Merchant merchant) {
        if (getById(merchant.getId()) == null) {
            throw new BizException("商家不存在");
        }
        if (!StringUtils.hasText(merchant.getName())) {
            throw new BizException("商家名称不能为空");
        }
        updateById(merchant);
        logService.add("修改", "修改商家 " + merchant.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return merchant;
    }

    public void toggleBlacklist(Long id) {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new BizException("商家不存在");
        }
        merchant.setBlacklisted(!Boolean.TRUE.equals(merchant.getBlacklisted()));
        updateById(merchant);
        logService.add("修改", (merchant.getBlacklisted() ? "拉黑" : "移出黑名单") + "商家 " + merchant.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }

    public void delete(Long id) {
        Merchant merchant = getById(id);
        if (merchant == null) {
            throw new BizException("商家不存在");
        }
        Long count = contractService.count(Wrappers.<Contract>lambdaQuery().eq(Contract::getMerchantId, id));
        if (count > 0) {
            throw new BizException("该商家名下还有 " + count + " 份合同，不能删除");
        }
        removeById(id);
        logService.add("删除", "删除商家 " + merchant.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }
}
