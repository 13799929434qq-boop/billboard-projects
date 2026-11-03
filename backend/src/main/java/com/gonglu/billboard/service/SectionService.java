package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.Section;
import com.gonglu.billboard.mapper.SectionMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SectionService extends ServiceImpl<SectionMapper, Section> {

    private final BillboardService billboardService;
    private final LogService logService;
    private final CacheService cacheService;

    public SectionService(@Lazy BillboardService billboardService, LogService logService, CacheService cacheService) {
        this.billboardService = billboardService;
        this.logService = logService;
        this.cacheService = cacheService;
    }

    public List<Section> listAll(String keyword) {
        return list(Wrappers.<Section>lambdaQuery()
                .and(StringUtils.hasText(keyword), q -> q.like(Section::getName, keyword)
                        .or().like(Section::getCode, keyword)
                        .or().like(Section::getRegion, keyword))
                .orderByAsc(Section::getId));
    }

    public Section create(Section section) {
        validate(section);
        Long exists = count(Wrappers.<Section>lambdaQuery().eq(Section::getCode, section.getCode()));
        if (exists > 0) {
            throw new BizException("路段编号 " + section.getCode() + " 已存在");
        }
        save(section);
        logService.add("新增", "新增路段 " + section.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return section;
    }

    public Section update(Section section) {
        if (getById(section.getId()) == null) {
            throw new BizException("路段不存在");
        }
        validate(section);
        Long exists = count(Wrappers.<Section>lambdaQuery()
                .eq(Section::getCode, section.getCode()).ne(Section::getId, section.getId()));
        if (exists > 0) {
            throw new BizException("路段编号 " + section.getCode() + " 已存在");
        }
        updateById(section);
        logService.add("修改", "修改路段 " + section.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
        return section;
    }

    public void delete(Long id) {
        Section section = getById(id);
        if (section == null) {
            throw new BizException("路段不存在");
        }
        Long count = billboardService.count(Wrappers.<Billboard>lambdaQuery().eq(Billboard::getSectionId, id));
        if (count > 0) {
            throw new BizException("该路段下还有 " + count + " 块广告牌，不能删除");
        }
        removeById(id);
        logService.add("删除", "删除路段 " + section.getName());
        cacheService.evict(CacheService.KEY_LOOKUP);
    }

    private void validate(Section s) {
        if (!StringUtils.hasText(s.getCode()) || !StringUtils.hasText(s.getName())
                || !StringUtils.hasText(s.getRegion()) || !StringUtils.hasText(s.getOperator())) {
            throw new BizException("路段编号、名称、地区、运营单位不能为空");
        }
    }
}
