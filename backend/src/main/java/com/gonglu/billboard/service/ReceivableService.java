package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.ContractItem;
import com.gonglu.billboard.entity.FundTransaction;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.entity.Receivable;
import com.gonglu.billboard.mapper.ReceivableMapper;
import com.gonglu.billboard.security.SecurityUtil;
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
 * 应收账单（应收账）：合同生效时按 年×牌 拆分；
 * 收款冲账与流水记账在同一事务中完成（两本账分离但联动）。
 */
@Service
public class ReceivableService extends ServiceImpl<ReceivableMapper, Receivable> {

    private final TransactionService transactionService;
    private final MerchantService merchantService;
    private final BillboardService billboardService;
    private final ContractService contractService;
    private final LogService logService;

    public ReceivableService(@Lazy TransactionService transactionService, @Lazy MerchantService merchantService,
                             @Lazy BillboardService billboardService, @Lazy ContractService contractService,
                             LogService logService) {
        this.transactionService = transactionService;
        this.merchantService = merchantService;
        this.billboardService = billboardService;
        this.contractService = contractService;
        this.logService = logService;
    }

    /** 合同生效时生成应收：年限 × 每块牌年租金 */
    public List<Receivable> generateFor(Contract contract, List<ContractItem> items) {
        int startYear = contract.getStartDate().getYear();
        List<Receivable> list = new java.util.ArrayList<>();
        for (int i = 0; i < contract.getYears(); i++) {
            int year = startYear + i;
            for (ContractItem item : items) {
                Receivable r = new Receivable();
                r.setContractId(contract.getId());
                r.setMerchantId(contract.getMerchantId());
                r.setBillboardId(item.getBillboardId());
                r.setYear(year);
                r.setAmount(item.getYearlyPrice());
                r.setReceived(BigDecimal.ZERO);
                r.setStatus("未收");
                list.add(r);
            }
        }
        return list;
    }

    public Map<Long, BigDecimal> receivedByContract(List<Long> contractIds) {
        if (contractIds.isEmpty()) {
            return Map.of();
        }
        return list(Wrappers.<Receivable>lambdaQuery().in(Receivable::getContractId, contractIds)).stream()
                .collect(Collectors.groupingBy(Receivable::getContractId,
                        Collectors.mapping(Receivable::getReceived,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    public List<Receivable> listAll(String keyword, String status, Integer year, Long merchantId) {
        List<Receivable> list = list(Wrappers.<Receivable>lambdaQuery()
                .eq(StringUtils.hasText(status), Receivable::getStatus, status)
                .eq(year != null, Receivable::getYear, year)
                .eq(merchantId != null, Receivable::getMerchantId, merchantId)
                .orderByAsc(Receivable::getYear).orderByAsc(Receivable::getId));
        Map<Long, Merchant> merchants = merchantService.list().stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m));
        Map<Long, Billboard> billboards = billboardService.list().stream()
                .collect(Collectors.toMap(Billboard::getId, b -> b));
        Map<Long, Contract> contracts = contractService.list().stream()
                .collect(Collectors.toMap(Contract::getId, c -> c));
        List<Receivable> result = list.stream().map(r -> {
            Merchant m = merchants.get(r.getMerchantId());
            Billboard b = billboards.get(r.getBillboardId());
            Contract c = contracts.get(r.getContractId());
            r.setMerchantName(m != null ? m.getName() : "-");
            r.setBillboardCode(b != null ? b.getCode() : "-");
            r.setContractCode(c != null ? c.getCode() : "-");
            return r;
        }).toList();
        if (StringUtils.hasText(keyword)) {
            result = result.stream().filter(r ->
                    r.getMerchantName().contains(keyword) || r.getContractCode().contains(keyword)
                            || r.getBillboardCode().contains(keyword)).toList();
        }
        return result;
    }

    /**
     * 收款冲账（支持部分缴纳）：
     * 一个事务内完成 ①冲减应收 ②改应收状态 ③写收入流水 ④记操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Receivable pay(Long id, BigDecimal amount) {
        // 行锁防止并发超收
        Receivable r = baseMapper.selectByIdForUpdate(id);
        if (r == null) {
            throw new BizException("应收账单不存在");
        }
        BigDecimal remain = r.getAmount().subtract(r.getReceived());
        if (remain.signum() <= 0) {
            throw new BizException("该账单已收齐，无需再收款");
        }
        if (amount.compareTo(remain) > 0) {
            throw new BizException("收款金额不能超过剩余应收（" + remain.toPlainString() + " 元）");
        }
        r.setReceived(r.getReceived().add(amount));
        r.refreshStatus();
        updateById(r);

        Contract c = contractService.getById(r.getContractId());
        Billboard b = billboardService.getById(r.getBillboardId());
        Merchant m = merchantService.getById(r.getMerchantId());

        FundTransaction tx = new FundTransaction();
        tx.setTxDate(LocalDate.now());
        tx.setDirection("收入");
        tx.setCategory("合同款");
        tx.setAmount(amount);
        tx.setMerchantId(r.getMerchantId());
        tx.setContractId(r.getContractId());
        tx.setSummary("合同 " + (c != null ? c.getCode() : r.getContractId()) + " " + r.getYear() + " 年度 "
                + (b != null ? b.getCode() : "") + " 租金");
        tx.setOperator(SecurityUtil.name());
        transactionService.save(tx);

        logService.add("收款登记", "收取 " + (m != null ? m.getName() : "") + " " + r.getYear() + " 年度租金 "
                + amount.toPlainString() + " 元（" + (b != null ? b.getCode() : "") + "）");
        return r;
    }
}
