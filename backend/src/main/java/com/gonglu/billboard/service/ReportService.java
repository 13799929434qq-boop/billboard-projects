package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.entity.ContractItem;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.mapper.FundTransactionMapper;
import com.gonglu.billboard.mapper.ReceivableMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 经营报表：全部由业务数据实时聚合，数字与系统业务数据同源。
 * 1. 广告牌数量表  2. 合同执行情况表  3. 收款情况表  4. 应收款统计表（含催收清单）
 */
@Service
public class ReportService {

    private final BillboardService billboardService;
    private final ContractService contractService;
    private final MerchantService merchantService;
    private final ReceivableMapper receivableMapper;
    private final FundTransactionMapper transactionMapper;

    public ReportService(BillboardService billboardService,
                         ContractService contractService, MerchantService merchantService,
                         ReceivableMapper receivableMapper, FundTransactionMapper transactionMapper) {
        this.billboardService = billboardService;
        this.contractService = contractService;
        this.merchantService = merchantService;
        this.receivableMapper = receivableMapper;
        this.transactionMapper = transactionMapper;
    }

    /** 报表 1：广告牌数量表 */
    public Map<String, Object> billboardStats(String keyword) {
        List<Billboard> list = billboardService.listAll(keyword, null, null, null, null);
        Map<String, Object> result = new HashMap<>();
        // 筛选后的汇总必须与明细、导出保持同一口径，避免页面出现“明细已筛选、数字仍为全量”。
        result.put("total", list.size());
        result.put("bySection", groupBillboards(list, Billboard::getSectionName));
        result.put("byOwnership", groupBillboards(list, Billboard::getOwnership));
        result.put("byLandNature", groupBillboards(list, Billboard::getLandNature));
        result.put("byStatus", groupBillboards(list, Billboard::getStatus));
        result.put("list", list);
        return result;
    }

    private List<Map<String, Object>> groupBillboards(List<Billboard> list,
                                                       java.util.function.Function<Billboard, String> classifier) {
        return list.stream()
                .collect(Collectors.groupingBy(b -> {
                    String value = classifier.apply(b);
                    return value == null || value.isBlank() ? "未填写" : value;
                }, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .toList();
    }

    /** 报表 2：合同执行情况表（每块牌在租/空闲、合同剩余时间、收款进度、临期提示） */
    public List<Map<String, Object>> contractExecution() {
        List<Contract> contracts = contractService.listAll(null, null, null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Contract c : contracts) {
            if ("草稿".equals(c.getStatus()) || "解除".equals(c.getStatus())) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("code", c.getCode());
            row.put("merchantName", c.getMerchantName());
            row.put("startDate", c.getStartDate());
            row.put("endDate", c.getEndDate());
            row.put("status", c.getDisplayStatus());
            row.put("billboardCount", c.getItems() != null ? c.getItems().size() : 0);
            row.put("billboardCodes", c.getItems() != null
                    ? c.getItems().stream().map(ContractItem::getBillboardCode).toList() : List.of());
            row.put("totalAmount", c.getTotalAmount());
            BigDecimal received = c.getReceivedAmount() != null ? c.getReceivedAmount() : BigDecimal.ZERO;
            row.put("receivedAmount", received);
            row.put("progress", c.getTotalAmount().signum() > 0
                    ? received.multiply(BigDecimal.valueOf(100)).divide(c.getTotalAmount(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            row.put("daysRemaining", c.getDaysRemaining());
            // 90 天内到期视为临期
            row.put("expiringSoon", "生效".equals(c.getStatus())
                    && c.getDaysRemaining() != null && c.getDaysRemaining() >= 0 && c.getDaysRemaining() <= 90);
            rows.add(row);
        }
        return rows;
    }

    /** 报表 2 补充：广告牌占用情况（每块牌在租/空闲/维护、所属生效合同、剩余天数、收款进度） */
    public List<Map<String, Object>> billboardOccupancy() {
        List<Billboard> billboards = billboardService.listAll(null, null, null, null, null);
        Map<String, Contract> contractByBillboard = new HashMap<>();
        for (Contract c : contractService.listAll(null, null, null)) {
            if (!"生效".equals(c.getStatus()) || c.getItems() == null) {
                continue;
            }
            for (ContractItem item : c.getItems()) {
                contractByBillboard.putIfAbsent(item.getBillboardCode(), c);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Billboard b : billboards) {
            Map<String, Object> row = new HashMap<>();
            row.put("code", b.getCode());
            row.put("sectionName", b.getSectionName());
            row.put("status", b.getStatus());
            Contract c = contractByBillboard.get(b.getCode());
            if (c != null) {
                row.put("contractCode", c.getCode());
                row.put("merchantName", c.getMerchantName());
                row.put("endDate", c.getEndDate());
                row.put("daysRemaining", c.getDaysRemaining());
                row.put("expiringSoon", c.getDaysRemaining() != null && c.getDaysRemaining() >= 0 && c.getDaysRemaining() <= 90);
                BigDecimal received = c.getReceivedAmount() != null ? c.getReceivedAmount() : BigDecimal.ZERO;
                row.put("progress", c.getTotalAmount().signum() > 0
                        ? received.multiply(BigDecimal.valueOf(100)).divide(c.getTotalAmount(), 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
            } else {
                row.put("contractCode", null);
                row.put("merchantName", b.getRenterName());
                row.put("endDate", null);
                row.put("daysRemaining", null);
                row.put("expiringSoon", false);
                row.put("progress", null);
            }
            rows.add(row);
        }
        return rows;
    }

    /** 报表 3：收款情况表（一段时间内的收入笔数、总额、按商家/合同/类别分布） */
    public Map<String, Object> collection(LocalDate from, LocalDate to) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> total = transactionMapper.sumIncome(from, to);
        result.put("count", total.getOrDefault("totalCount", 0));
        result.put("amount", total.getOrDefault("totalAmount", BigDecimal.ZERO));
        result.put("byMerchant", transactionMapper.incomeByMerchant(from, to));
        result.put("byCategory", transactionMapper.incomeByCategory(from, to));
        result.put("byContract", transactionMapper.incomeByContract(from, to));
        return result;
    }

    /** 报表 4：应收款统计表（应收/已收/欠款汇总 + 商家欠款 + 催收清单） */
    public Map<String, Object> receivableStats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> total = receivableMapper.sumAll();
        result.put("totalAmount", total.getOrDefault("totalAmount", BigDecimal.ZERO));
        result.put("totalReceived", total.getOrDefault("totalReceived", BigDecimal.ZERO));
        result.put("totalArrears", total.getOrDefault("totalArrears", BigDecimal.ZERO));
        result.put("byMerchant", receivableMapper.groupByMerchant().stream().map(source -> {
            Map<String, Object> row = new HashMap<>(source);
            row.put("name", source.get("merchantName"));
            row.put("totalAmount", source.get("amount"));
            row.put("receivedAmount", source.get("received"));
            return row;
        }).toList());
        result.put("noticeList", receivableMapper.collectionNoticeList());
        return result;
    }

    /** 广告牌明细导出用数据（导出单元格不允许 null，统一空串兜底） */
    public List<Map<String, Object>> billboardRows(String keyword) {
        return billboardService.listAll(keyword, null, null, null, null).stream().map(b -> {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("code", nvl(b.getCode()));
            row.put("sectionName", nvl(b.getSectionName()));
            row.put("side", nvl(b.getSide()));
            row.put("stake", nvl(b.getStakeActual()));
            row.put("spec", nvl(b.getSpec()));
            row.put("size", (b.getWidth() != null ? b.getWidth() : "?") + "×" + (b.getHeight() != null ? b.getHeight() : "?") + "米");
            row.put("ownership", nvl(b.getOwnership()));
            row.put("landNature", nvl(b.getLandNature()));
            row.put("status", nvl(b.getStatus()));
            row.put("renterName", b.getRenterName() != null ? b.getRenterName() : "-");
            return row;
        }).collect(Collectors.toList());
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
