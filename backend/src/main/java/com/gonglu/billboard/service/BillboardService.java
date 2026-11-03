package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.ContractItem;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.entity.Section;
import com.gonglu.billboard.mapper.BillboardMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BillboardService extends ServiceImpl<BillboardMapper, Billboard> {

    private final SectionService sectionService;
    private final ContractService contractService;
    private final MerchantService merchantService;
    private final LogService logService;
    private final CacheService cacheService;

    public BillboardService(SectionService sectionService, @Lazy ContractService contractService,
                            @Lazy MerchantService merchantService, LogService logService, CacheService cacheService) {
        this.sectionService = sectionService;
        this.contractService = contractService;
        this.merchantService = merchantService;
        this.logService = logService;
        this.cacheService = cacheService;
    }

    public List<Billboard> listAll(String keyword, Long sectionId, String status, String ownership, String landNature) {
        List<Billboard> list = list(Wrappers.<Billboard>lambdaQuery()
                .and(StringUtils.hasText(keyword), q -> q.like(Billboard::getCode, keyword)
                        .or().like(Billboard::getStakeContract, keyword)
                        .or().like(Billboard::getStakeActual, keyword))
                .eq(sectionId != null, Billboard::getSectionId, sectionId)
                .eq(StringUtils.hasText(status), Billboard::getStatus, status)
                .eq(StringUtils.hasText(ownership), Billboard::getOwnership, ownership)
                .eq(StringUtils.hasText(landNature), Billboard::getLandNature, landNature)
                .orderByAsc(Billboard::getId));
        fillExtra(list);
        return list;
    }

    /** 填充路段名称、当前承租商家与合同 */
    public void fillExtra(List<Billboard> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, String> sectionNames = sectionService.list().stream()
                .collect(Collectors.toMap(Section::getId, Section::getName));
        LocalDate today = LocalDate.now();
        List<Contract> active = contractService.list(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getStatus, "生效")
                .le(Contract::getStartDate, today)
                .ge(Contract::getEndDate, today));
        Map<Long, Contract> billboardContract = active.stream()
                .flatMap(c -> contractService.itemsOf(c.getId()).stream().map(i -> Map.entry(i.getBillboardId(), c)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        Map<Long, String> merchantNames = merchantService.list().stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName));
        for (Billboard b : list) {
            b.setSectionName(sectionNames.get(b.getSectionId()));
            Contract c = billboardContract.get(b.getId());
            if (c != null) {
                b.setContractCode(c.getCode());
                b.setRenterName(merchantNames.get(c.getMerchantId()));
            }
        }
    }

    public Billboard create(Billboard billboard) {
        validate(billboard);
        Long exists = count(Wrappers.<Billboard>lambdaQuery().eq(Billboard::getCode, billboard.getCode()));
        if (exists > 0) {
            throw new BizException("广告牌编号 " + billboard.getCode() + " 已存在");
        }
        if (!StringUtils.hasText(billboard.getStatus())) {
            billboard.setStatus("空闲");
        }
        save(billboard);
        logService.add("新增", "新增广告牌 " + billboard.getCode());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return billboard;
    }

    public Billboard update(Billboard billboard) {
        if (getById(billboard.getId()) == null) {
            throw new BizException("广告牌不存在");
        }
        validate(billboard);
        Long exists = count(Wrappers.<Billboard>lambdaQuery()
                .eq(Billboard::getCode, billboard.getCode()).ne(Billboard::getId, billboard.getId()));
        if (exists > 0) {
            throw new BizException("广告牌编号 " + billboard.getCode() + " 已存在");
        }
        updateById(billboard);
        logService.add("修改", "修改广告牌 " + billboard.getCode());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return billboard;
    }

    public void delete(Long id) {
        Billboard billboard = getById(id);
        if (billboard == null) {
            throw new BizException("广告牌不存在");
        }
        boolean inContract = contractService.itemsOf(null).stream()
                .anyMatch(i -> i.getBillboardId().equals(id));
        if (inContract) {
            throw new BizException("该广告牌已关联合同，不能删除（可先解除合同）");
        }
        removeById(id);
        logService.add("删除", "删除广告牌 " + billboard.getCode());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }

    /** 批量更新状态（合同生效/解除时联动） */
    public void updateStatus(List<Long> billboardIds, String status) {
        for (Long id : billboardIds) {
            Billboard b = new Billboard();
            b.setId(id);
            b.setStatus(status);
            updateById(b);
        }
    }

    public Map<Long, Billboard> mapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return listByIds(ids).stream().collect(Collectors.toMap(Billboard::getId, Function.identity()));
    }

    private void validate(Billboard b) {
        if (!StringUtils.hasText(b.getCode()) || b.getSectionId() == null) {
            throw new BizException("广告牌编号、所属路段不能为空");
        }
    }
}
