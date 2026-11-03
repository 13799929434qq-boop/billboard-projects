package com.gonglu.billboard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.SysOperationLog;
import com.gonglu.billboard.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志（仅管理员）")
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "操作日志分页查询")
    @GetMapping
    public R<Page<SysOperationLog>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String username) {
        return R.ok(logService.pageQuery(page, size, username));
    }
}
