package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.SysDict;
import com.gonglu.billboard.entity.SysDictItem;
import com.gonglu.billboard.security.SecurityUtil;
import com.gonglu.billboard.service.DictService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "数据字典")
@RestController
@RequestMapping("/api/dicts")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "全部字典（登录即可读，供下拉框使用）")
    @GetMapping
    public R<List<SysDict>> list() {
        return R.ok(dictService.listForRole(SecurityUtil.current().role()));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/{type}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysDictItem> addItem(@PathVariable String type, @RequestBody Map<String, String> body) {
        return R.ok(dictService.addItem(type, body.get("label"), SecurityUtil.current().role()));
    }

    @Operation(summary = "修改字典项名称与排序")
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<SysDictItem> updateItem(@PathVariable Long id, @RequestBody SysDictItem input) {
        return R.ok(dictService.updateItem(id, input));
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id, SecurityUtil.current().role());
        return R.ok();
    }
}
