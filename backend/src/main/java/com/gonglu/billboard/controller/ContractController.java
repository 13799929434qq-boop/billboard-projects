package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.dto.ContractSaveReq;
import com.gonglu.billboard.entity.Contract;
import com.gonglu.billboard.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "合同列表（含明细、收款进度）")
    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<List<Contract>> list(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long merchantId) {
        return R.ok(contractService.listAll(keyword, status, merchantId));
    }

    @Operation(summary = "签订合同（支持一合同多牌，生效时自动生成应收）")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<Contract> create(@Valid @RequestBody ContractSaveReq req) {
        return R.ok(contractService.create(req));
    }

    @Operation(summary = "修改合同（草稿可改绑牌，生效需校验重叠）")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Contract> update(@PathVariable Long id, @Valid @RequestBody ContractSaveReq req) {
        req.setId(id);
        return R.ok(contractService.update(req));
    }

    @Operation(summary = "解除合同")
    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Void> terminate(@PathVariable Long id) {
        contractService.terminate(id);
        return R.ok();
    }

    @Operation(summary = "删除合同（仅草稿/解除且无收款记录）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return R.ok();
    }
}
