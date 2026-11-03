package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.dto.ContractSaveReq;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.ContractItem;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.entity.Receivable;
import com.gonglu.billboard.mapper.ContractItemMapper;
import com.gonglu.billboard.mapper.ContractMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同管理核心规则：
 * 1. 一份合同绑定多块广告牌，每块牌每年价格可不同（ContractItem）
 * 2. 同一块牌在生效合同中时间不得重叠
 * 3. 合同生效时按「年 × 牌」自动生成应收账单
 */
@Service
public class ContractService extends ServiceImpl<ContractMapper, Contract> {

    private final ContractItemMapper itemMapper;
    private final MerchantService merchantService;
    private final BillboardService billboardService;
    private final ReceivableService receivableService;
    private final LogService logService;
    private final CacheService cacheService;

    public ContractService(ContractItemMapper itemMapper, @Lazy MerchantService merchantService,
                           @Lazy BillboardService billboardService, @Lazy ReceivableService receivableService,
                           LogService logService, CacheService cacheService) {
        this.itemMapper = itemMapper;
        this.merchantService = merchantService;
        this.billboardService = billboardService;
        this.receivableService = receivableService;
        this.logService = logService;
        this.cacheService = cacheService;
    }

    public List<ContractItem> itemsOf(Long contractId) {
        return itemMapper.selectList(Wrappers.<ContractItem>lambdaQuery()
                .eq(contractId != null, ContractItem::getContractId, contractId));
    }

    public List<Contract> listAll(String keyword, String status, Long merchantId) {
        List<Contract> list = list(Wrappers.<Contract>lambdaQuery()
                .like(StringUtils.hasText(keyword), Contract::getCode, keyword)
                .eq(StringUtils.hasText(status), Contract::getStatus, status)
                .eq(merchantId != null, Contract::getMerchantId, merchantId)
                .orderByDesc(Contract::getId));
        fillExtra(list);
        return list;
    }

    public void fillExtra(List<Contract> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, String> merchantNames = merchantService.list().stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName));
        List<Long> ids = list.stream().map(Contract::getId).toList();
        Map<Long, List<ContractItem>> itemsMap = itemMapper.selectList(
                        Wrappers.<ContractItem>lambdaQuery().in(ContractItem::getContractId, ids))
                .stream().collect(Collectors.groupingBy(ContractItem::getContractId));
        Map<Long, BigDecimal> receivedMap = receivableService.receivedByContract(ids);
        Map<Long, Billboard> billboardMap = billboardService.mapByIds(
                itemsMap.values().stream().flatMap(List::stream).map(ContractItem::getBillboardId).distinct().toList());
        for (Contract c : list) {
            c.setMerchantName(merchantNames.get(c.getMerchantId()));
            List<ContractItem> items = itemsMap.getOrDefault(c.getId(), List.of());
            for (ContractItem item : items) {
                Billboard b = billboardMap.get(item.getBillboardId());
                if (b != null) {
                    item.setBillboardCode(b.getCode());
                    item.setBillboardLocation(b.getSide() + " " + b.getStakeActual());
                }
            }
            c.setItems(items);
            c.setReceivedAmount(receivedMap.getOrDefault(c.getId(), BigDecimal.ZERO));
            c.computeDerived();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Contract create(ContractSaveReq req) {
        validate(req);
        Merchant merchant = merchantService.getById(req.getMerchantId());
        if (merchant == null) {
            throw new BizException("商家不存在");
        }
        if ("生效".equals(req.getStatus())) {
            if (Boolean.TRUE.equals(merchant.getBlacklisted())) {
                throw new BizException("商家「" + merchant.getName() + "」在黑名单中，不能签订生效合同");
            }
            checkOverlap(req, null);
        }
        Contract contract = new Contract();
        contract.setCode(StringUtils.hasText(req.getCode()) ? req.getCode() : nextCode(req.getStartDate()));
        contract.setMerchantId(req.getMerchantId());
        contract.setSignDate(req.getSignDate());
        contract.setStartDate(req.getStartDate());
        contract.setEndDate(req.getEndDate());
        contract.setYears(computeYears(req.getStartDate(), req.getEndDate()));
        contract.setTotalAmount(computeTotal(req));
        contract.setStatus(req.getStatus());
        contract.setRemark(req.getRemark());
        save(contract);
        saveItems(contract.getId(), req.getItems());
        if ("生效".equals(contract.getStatus())) {
            activate(contract);
        }
        logService.add("新增", "签订合同 " + contract.getCode() + "（商家：" + merchant.getName()
                + "，" + req.getItems().size() + " 块牌，总额 " + contract.getTotalAmount() + " 元）");
        cacheService.evict(CacheService.KEY_LOOKUP);
        return contract;
    }

    @Transactional(rollbackFor = Exception.class)
    public Contract update(ContractSaveReq req) {
        Contract contract = getById(req.getId());
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if ("解除".equals(contract.getStatus()) || "到期".equals(contract.getDisplayStatus())) {
            throw new BizException("已解除/已到期的合同不能再修改");
        }
        boolean activating = "生效".equals(req.getStatus()) && !"生效".equals(contract.getStatus());
        if (activating) {
            Merchant merchant = merchantService.getById(contract.getMerchantId());
            if (merchant != null && Boolean.TRUE.equals(merchant.getBlacklisted())) {
                throw new BizException("商家「" + merchant.getName() + "」在黑名单中，合同不能生效");
            }
        }
        validate(req);
        if ("生效".equals(req.getStatus())) {
            checkOverlap(req, contract.getId());
        }
        contract.setStartDate(req.getStartDate());
        contract.setEndDate(req.getEndDate());
        contract.setSignDate(req.getSignDate());
        contract.setYears(computeYears(req.getStartDate(), req.getEndDate()));
        contract.setTotalAmount(computeTotal(req));
        contract.setRemark(req.getRemark());
        contract.setStatus(req.getStatus());
        // 草稿允许改绑牌；生效合同不允许改明细（避免应收错位）
        if (!"生效".equals(contract.getStatus()) || activating) {
            itemMapper.delete(Wrappers.<ContractItem>lambdaQuery().eq(ContractItem::getContractId, contract.getId()));
            saveItems(contract.getId(), req.getItems());
        }
        updateById(contract);
        if (activating) {
            activate(contract);
        }
        logService.add("修改", "修改合同 " + contract.getCode() + (activating ? "，状态变更为生效" : ""));
        cacheService.evict(CacheService.KEY_LOOKUP);
        return contract;
    }

    /** 合同生效：生成应收 + 广告牌置为在租 */
    private void activate(Contract contract) {
        List<Receivable> receivables = receivableService.generateFor(contract, itemsOf(contract.getId()));
        receivableService.saveBatch(receivables);
        billboardService.updateStatus(
                itemsOf(contract.getId()).stream().map(ContractItem::getBillboardId).toList(), "在租");
    }

    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long id) {
        Contract contract = getById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if (!"生效".equals(contract.getStatus())) {
            throw new BizException("只有生效合同可以解除");
        }
        contract.setStatus("解除");
        updateById(contract);
        // 已产生的应收保留（债务仍在），广告牌置回空闲
        billboardService.updateStatus(
                itemsOf(id).stream().map(ContractItem::getBillboardId).toList(), "空闲");
        logService.add("解除", "解除合同 " + contract.getCode());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Contract contract = getById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if ("生效".equals(contract.getStatus())) {
            throw new BizException("生效合同不能删除，请先解除");
        }
        Long paid = receivableService.count(Wrappers.<Receivable>lambdaQuery()
                .eq(Receivable::getContractId, id).gt(Receivable::getReceived, BigDecimal.ZERO));
        if (paid > 0) {
            throw new BizException("该合同已有收款记录，不能删除");
        }
        receivableService.remove(Wrappers.<Receivable>lambdaQuery().eq(Receivable::getContractId, id));
        itemMapper.delete(Wrappers.<ContractItem>lambdaQuery().eq(ContractItem::getContractId, id));
        removeById(id);
        logService.add("删除", "删除合同 " + contract.getCode());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }

    /** 校验同一块牌在生效合同中时间不得重叠 */
    private void checkOverlap(ContractSaveReq req, Long excludeId) {
        List<Long> billboardIds = req.getItems().stream().map(ContractSaveReq.Item::getBillboardId).toList();
        List<Contract> active = list(Wrappers.<Contract>lambdaQuery()
                .eq(Contract::getStatus, "生效")
                .ne(excludeId != null, Contract::getId, excludeId)
                .le(Contract::getStartDate, req.getEndDate())
                .ge(Contract::getEndDate, req.getStartDate()));
        for (Contract c : active) {
            for (ContractItem item : itemsOf(c.getId())) {
                if (billboardIds.contains(item.getBillboardId())) {
                    Billboard b = billboardService.getById(item.getBillboardId());
                    throw new BizException("广告牌 " + (b != null ? b.getCode() : item.getBillboardId())
                            + " 已在生效合同 " + c.getCode() + " 中，且时间重叠");
                }
            }
        }
    }

    private void validate(ContractSaveReq req) {
        if (!req.getEndDate().isAfter(req.getStartDate())) {
            throw new BizException("到期日期必须晚于起租日期");
        }
        List<Long> ids = req.getItems().stream().map(ContractSaveReq.Item::getBillboardId).distinct().toList();
        if (ids.size() != req.getItems().size()) {
            throw new BizException("同一块广告牌在合同中重复绑定");
        }
        for (ContractSaveReq.Item item : req.getItems()) {
            if (item.getYearlyPrice() == null || item.getYearlyPrice().signum() <= 0) {
                throw new BizException("每块广告牌的年租金必须大于 0");
            }
            Billboard b = billboardService.getById(item.getBillboardId());
            if (b == null) {
                throw new BizException("广告牌不存在：" + item.getBillboardId());
            }
            if ("停用".equals(b.getStatus())) {
                throw new BizException("广告牌 " + b.getCode() + " 已停用，不能签约");
            }
        }
    }

    private BigDecimal computeTotal(ContractSaveReq req) {
        BigDecimal yearly = req.getItems().stream()
                .map(ContractSaveReq.Item::getYearlyPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return yearly.multiply(BigDecimal.valueOf(computeYears(req.getStartDate(), req.getEndDate())));
    }

    /** 年限按起止年份差计算（跨年按整年计） */
    private int computeYears(LocalDate start, LocalDate end) {
        return end.getYear() - start.getYear() + 1;
    }

    private void saveItems(Long contractId, List<ContractSaveReq.Item> items) {
        for (ContractSaveReq.Item item : items) {
            ContractItem ci = new ContractItem();
            ci.setContractId(contractId);
            ci.setBillboardId(item.getBillboardId());
            ci.setYearlyPrice(item.getYearlyPrice());
            itemMapper.insert(ci);
        }
    }

    /** 自动生成合同编号：HT-年份-序号 */
    private String nextCode(LocalDate startDate) {
        int year = startDate != null ? startDate.getYear() : LocalDate.now().getYear();
        Long count = count(Wrappers.<Contract>lambdaQuery().likeRight(Contract::getCode, "HT-" + year + "-"));
        return String.format("HT-%d-%03d", year, count + 1);
    }
}
