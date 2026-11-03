package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.BillboardChange;
import com.gonglu.billboard.service.BillboardChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "广告牌变动记录")
@RestController
@RequestMapping("/api/billboard-changes")
@PreAuthorize("hasRole('ARCHIVIST')")
public class BillboardChangeController {
    private final BillboardChangeService service;

    public BillboardChangeController(BillboardChangeService service) {
        this.service = service;
    }

    @Operation(summary = "变动记录列表")
    @GetMapping
    public R<List<BillboardChange>> list(@RequestParam(required = false) Long billboardId,
                                         @RequestParam(required = false) String changeType) {
        return R.ok(service.listAll(billboardId, changeType));
    }

    @Operation(summary = "登记广告牌变动（暂不包含审批流程）")
    @PostMapping
    public R<BillboardChange> create(@RequestBody BillboardChange row) {
        return R.ok(service.create(row));
    }
}

