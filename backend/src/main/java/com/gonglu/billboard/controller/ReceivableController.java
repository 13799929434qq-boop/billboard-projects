package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.dto.PayReq;
import com.gonglu.billboard.entity.Receivable;
import com.gonglu.billboard.service.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "应收管理")
@RestController
@RequestMapping("/api/receivables")
public class ReceivableController {

    private final ReceivableService receivableService;

    public ReceivableController(ReceivableService receivableService) {
        this.receivableService = receivableService;
    }

    @Operation(summary = "应收列表（年×牌，含商家/合同/牌号）")
    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<List<Receivable>> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Long merchantId) {
        return R.ok(receivableService.listAll(keyword, status, year, merchantId));
    }

    @Operation(summary = "收款登记（支持部分缴纳，冲减应收并同步写流水）")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Receivable> pay(@PathVariable Long id, @Valid @RequestBody PayReq req) {
        return R.ok(receivableService.pay(id, req.getAmount()));
    }
}
