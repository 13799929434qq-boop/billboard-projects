package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.FundTransaction;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.mapper.FundTransactionMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资金流水（实收实付账）。支持挂账：merchantId/contractId 均可为空。
 */
@Service
public class TransactionService extends ServiceImpl<FundTransactionMapper, FundTransaction> {

    private final MerchantService merchantService;
    private final ContractService contractService;
    private final LogService logService;

    public TransactionService(@Lazy MerchantService merchantService, @Lazy ContractService contractService,
                              LogService logService) {
        this.merchantService = merchantService;
        this.contractService = contractService;
        this.logService = logService;
    }

    public List<FundTransaction> listAll(String direction, String category, String keyword, LocalDate from, LocalDate to) {
        List<FundTransaction> list = list(Wrappers.<FundTransaction>lambdaQuery()
                .eq(StringUtils.hasText(direction), FundTransaction::getDirection, direction)
                .eq(StringUtils.hasText(category), FundTransaction::getCategory, category)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(FundTransaction::getSummary, keyword)
                        .or()
                        .like(FundTransaction::getOperator, keyword))
                .ge(from != null, FundTransaction::getTxDate, from)
                .le(to != null, FundTransaction::getTxDate, to)
                .orderByDesc(FundTransaction::getTxDate).orderByDesc(FundTransaction::getId));
        Map<Long, String> merchantNames = merchantService.list().stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName));
        Map<Long, String> contractCodes = contractService.list().stream()
                .collect(Collectors.toMap(Contract::getId, Contract::getCode));
        for (FundTransaction t : list) {
            t.setMerchantName(t.getMerchantId() != null ? merchantNames.get(t.getMerchantId()) : null);
            t.setContractCode(t.getContractId() != null ? contractCodes.get(t.getContractId()) : null);
        }
        return list;
    }

    /** 手工登记流水（支持挂账：商家/合同均可为空，后续再认） */
    public FundTransaction create(FundTransaction tx) {
        if (!StringUtils.hasText(tx.getDirection()) || !StringUtils.hasText(tx.getCategory())) {
            throw new BizException("收付方向、类别不能为空");
        }
        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        if (tx.getTxDate() == null) {
            tx.setTxDate(LocalDate.now());
        }
        if (tx.getMerchantId() != null && merchantService.getById(tx.getMerchantId()) == null) {
            throw new BizException("商家不存在");
        }
        if (tx.getContractId() != null && contractService.getById(tx.getContractId()) == null) {
            throw new BizException("合同不存在");
        }
        tx.setOperator(com.gonglu.billboard.security.SecurityUtil.name());
        save(tx);
        logService.add("登记流水", tx.getDirection() + " " + tx.getCategory() + " "
                + tx.getAmount().toPlainString() + " 元" + (StringUtils.hasText(tx.getSummary()) ? "（" + tx.getSummary() + "）" : ""));
        return tx;
    }

    public FundTransaction updateManual(Long id, FundTransaction input) {
        FundTransaction tx = getById(id);
        if (tx == null) {
            throw new BizException("流水不存在");
        }
        if (isAutoReceipt(tx)) {
            throw new BizException("合同收款流水由应收账单自动生成，不能单独修改");
        }
        if (!StringUtils.hasText(input.getDirection()) || !StringUtils.hasText(input.getCategory())) {
            throw new BizException("收付方向、类别不能为空");
        }
        if (input.getAmount() == null || input.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于 0");
        }
        tx.setTxDate(input.getTxDate());
        tx.setDirection(input.getDirection());
        tx.setCategory(input.getCategory());
        tx.setAmount(input.getAmount());
        tx.setMerchantId(input.getMerchantId());
        tx.setContractId(input.getContractId());
        tx.setSummary(input.getSummary());
        updateById(tx);
        logService.add("修改流水", "修改手工流水 " + id + "：" + tx.getDirection() + " " + tx.getCategory()
                + " " + tx.getAmount().toPlainString() + " 元");
        return tx;
    }

    private boolean isAutoReceipt(FundTransaction tx) {
        return "合同款".equals(tx.getCategory()) && "收入".equals(tx.getDirection()) && tx.getContractId() != null;
    }

    public void delete(Long id) {
        FundTransaction tx = getById(id);
        if (tx == null) {
            throw new BizException("流水不存在");
        }
        if (isAutoReceipt(tx)) {
            throw new BizException("合同收款流水由收款登记自动生成，不能单独删除");
        }
        removeById(id);
        logService.add("删除流水", "删除流水：" + tx.getDirection() + " " + tx.getCategory() + " " + tx.getAmount() + " 元");
    }
}
