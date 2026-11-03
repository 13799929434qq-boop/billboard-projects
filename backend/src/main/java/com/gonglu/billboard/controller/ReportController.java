package com.gonglu.billboard.controller;

import com.alibaba.excel.EasyExcel;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.common.R;
import com.gonglu.billboard.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "经营报表（财务/领导/管理员）")
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('LEADER')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "广告牌数量表")
    @GetMapping("/billboards")
    public R<Map<String, Object>> billboardStats(@RequestParam(required = false) String keyword) {
        return R.ok(reportService.billboardStats(keyword));
    }

    @Operation(summary = "合同执行情况表")
    @GetMapping("/contracts")
    public R<List<Map<String, Object>>> contractExecution() {
        return R.ok(reportService.contractExecution());
    }

    @Operation(summary = "广告牌占用情况（合同执行-广告牌视角）")
    @GetMapping("/occupancy")
    public R<List<Map<String, Object>>> billboardOccupancy() {
        return R.ok(reportService.billboardOccupancy());
    }

    @Operation(summary = "收款情况表")
    @GetMapping("/collection")
    public R<Map<String, Object>> collection(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(reportService.collection(from, to));
    }

    @Operation(summary = "应收款统计表（含催收清单）")
    @GetMapping("/receivables")
    public R<Map<String, Object>> receivableStats() {
        return R.ok(reportService.receivableStats());
    }

    @Operation(summary = "报表导出 Excel（xlsx）")
    @GetMapping("/export")
    @SuppressWarnings("unchecked")
    public void export(@RequestParam String type,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       HttpServletResponse response) throws IOException {
        List<List<String>> head;
        List<List<Object>> rows;
        String filename;
        switch (type) {
            case "billboard" -> {
                filename = "广告牌数量表";
                head = heads("编号", "路段", "侧别", "实际桩号", "规格", "版面尺寸", "产权", "用地性质", "状态", "承租商家");
                rows = reportService.billboardRows(keyword).stream()
                        .map(r -> List.<Object>of(r.get("code"), r.get("sectionName"), r.get("side"), r.get("stake"),
                                r.get("spec"), r.get("size"), r.get("ownership"), r.get("landNature"),
                                r.get("status"), r.get("renterName")))
                        .toList();
            }
            case "execution" -> {
                filename = "合同执行情况表";
                head = heads("合同编号", "商家", "起租日期", "到期日期", "状态", "广告牌数", "合同总额", "已收金额", "收款进度%", "剩余天数");
                rows = reportService.contractExecution().stream()
                        .map(r -> List.<Object>of(str(r.get("code")), str(r.get("merchantName")), str(r.get("startDate")),
                                str(r.get("endDate")), str(r.get("status")), r.get("billboardCount"), r.get("totalAmount"),
                                r.get("receivedAmount"), r.get("progress"), r.get("daysRemaining")))
                        .toList();
            }
            case "occupancy" -> {
                filename = "广告牌占用情况表";
                head = heads("广告牌编号", "所属路段", "当前状态", "生效合同", "承租商家", "合同到期日", "剩余天数", "收款进度%");
                rows = reportService.billboardOccupancy().stream()
                        .map(r -> List.<Object>of(str(r.get("code")), str(r.get("sectionName")), str(r.get("status")),
                                str(r.get("contractCode")), str(r.get("merchantName")), str(r.get("endDate")),
                                r.get("daysRemaining"), r.get("progress")))
                        .toList();
            }
            case "collection" -> {
                filename = "收款情况表";
                head = heads("分布维度", "名称", "笔数", "金额(元)");
                Map<String, Object> data = reportService.collection(from, to);
                rows = new java.util.ArrayList<>();
                appendGroup(rows, "按商家", (List<Map<String, Object>>) data.get("byMerchant"));
                appendGroup(rows, "按类别", (List<Map<String, Object>>) data.get("byCategory"));
                appendGroup(rows, "按合同", (List<Map<String, Object>>) data.get("byContract"));
            }
            case "receivable" -> {
                filename = "应收款催收清单";
                head = heads("商家", "联系人", "电话", "合同编号", "广告牌编号", "路段", "年度", "应收(元)", "已收(元)", "欠款(元)");
                Map<String, Object> data = reportService.receivableStats();
                rows = ((List<Map<String, Object>>) data.get("noticeList")).stream()
                        .map(r -> List.<Object>of(str(r.get("merchantName")), str(r.get("contact")), str(r.get("phone")),
                                str(r.get("contractCode")), str(r.get("billboardCode")), str(r.get("sectionName")),
                                r.get("year"), r.get("amount"), r.get("received"), r.get("arrears")))
                        .toList();
            }
            default -> throw new BizException("未知报表类型：" + type);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(filename + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded);
        EasyExcel.write(response.getOutputStream()).head(head).sheet(filename).doWrite(rows);
    }

    private List<List<String>> heads(String... names) {
        return java.util.Arrays.stream(names).map(List::of).toList();
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private void appendGroup(List<List<Object>> rows, String dimension, List<Map<String, Object>> group) {
        if (group == null) {
            return;
        }
        for (Map<String, Object> g : group) {
            rows.add(List.of(dimension, str(g.get("name")), g.get("count"), g.get("amount")));
        }
    }
}
