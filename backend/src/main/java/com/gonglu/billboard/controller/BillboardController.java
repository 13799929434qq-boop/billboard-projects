package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.service.BillboardService;
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

@Tag(name = "广告牌档案")
@RestController
@RequestMapping("/api/billboards")
public class BillboardController {

    private final BillboardService billboardService;

    public BillboardController(BillboardService billboardService) {
        this.billboardService = billboardService;
    }

    @Operation(summary = "广告牌列表（含路段、承租商家）")
    @GetMapping
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<List<Billboard>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Long sectionId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String ownership,
                                   @RequestParam(required = false) String landNature) {
        return R.ok(billboardService.listAll(keyword, sectionId, status, ownership, landNature));
    }

    @Operation(summary = "新增广告牌")
    @PostMapping
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Billboard> create(@RequestBody Billboard billboard) {
        return R.ok(billboardService.create(billboard));
    }

    @Operation(summary = "修改广告牌")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Billboard> update(@PathVariable Long id, @RequestBody Billboard billboard) {
        billboard.setId(id);
        return R.ok(billboardService.update(billboard));
    }

    @Operation(summary = "删除广告牌（已关联合同时禁止）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Void> delete(@PathVariable Long id) {
        billboardService.delete(id);
        return R.ok();
    }
}
