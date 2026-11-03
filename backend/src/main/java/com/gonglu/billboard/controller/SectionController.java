package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.entity.Section;
import com.gonglu.billboard.service.SectionService;
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

@Tag(name = "路段档案")
@RestController
@RequestMapping("/api/sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @Operation(summary = "路段列表")
    @GetMapping
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<List<Section>> list(@RequestParam(required = false) String keyword) {
        return R.ok(sectionService.listAll(keyword));
    }

    @Operation(summary = "新增路段")
    @PostMapping
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Section> create(@RequestBody Section section) {
        return R.ok(sectionService.create(section));
    }

    @Operation(summary = "修改路段")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Section> update(@PathVariable Long id, @RequestBody Section section) {
        section.setId(id);
        return R.ok(sectionService.update(section));
    }

    @Operation(summary = "删除路段（路段下有广告牌时禁止）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ARCHIVIST')")
    public R<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return R.ok();
    }
}
