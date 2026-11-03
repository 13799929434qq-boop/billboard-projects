package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.FundTransaction;
import com.gonglu.billboard.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "资金流水")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "流水列表（可按方向/类别/日期范围筛选）")
    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<List<FundTransaction>> list(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(transactionService.listAll(direction, category, keyword, from, to));
    }

    @Operation(summary = "手工登记流水（支持挂账：商家/合同可留空）")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<FundTransaction> create(@RequestBody FundTransaction tx) {
        return R.ok(transactionService.create(tx));
    }

    @Operation(summary = "修改手工登记流水（自动收款流水禁止修改）")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<FundTransaction> update(@PathVariable Long id, @RequestBody FundTransaction tx) {
        return R.ok(transactionService.updateManual(id, tx));
    }

    @Operation(summary = "删除流水（合同收款流水禁止单独删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return R.ok();
    }
}
