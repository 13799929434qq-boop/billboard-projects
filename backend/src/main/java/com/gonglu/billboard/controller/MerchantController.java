package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.Merchant;
import com.gonglu.billboard.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "商家档案")
@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Operation(summary = "商家列表（含欠款合计）")
    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<List<Merchant>> list(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Boolean blacklisted) {
        return R.ok(merchantService.listAll(keyword, blacklisted));
    }

    @Operation(summary = "新增商家")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE')")
    public R<Merchant> create(@RequestBody Merchant merchant) {
        return R.ok(merchantService.create(merchant));
    }

    @Operation(summary = "修改商家")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Merchant> update(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchant.setId(id);
        return R.ok(merchantService.update(merchant));
    }

    @Operation(summary = "拉黑 / 移出黑名单")
    @PutMapping("/{id}/blacklist")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Void> toggleBlacklist(@PathVariable Long id) {
        merchantService.toggleBlacklist(id);
        return R.ok();
    }

    @Operation(summary = "删除商家（名下有合同时禁止）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FINANCE')")
    public R<Void> delete(@PathVariable Long id) {
        merchantService.delete(id);
        return R.ok();
    }
}
