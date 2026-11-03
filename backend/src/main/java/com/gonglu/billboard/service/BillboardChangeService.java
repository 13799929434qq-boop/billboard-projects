package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.Billboard;
import com.gonglu.billboard.entity.BillboardChange;
import com.gonglu.billboard.mapper.BillboardChangeMapper;
import com.gonglu.billboard.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class BillboardChangeService extends ServiceImpl<BillboardChangeMapper, BillboardChange> {
    private final BillboardService billboardService;
    private final LogService logService;

    public BillboardChangeService(BillboardService billboardService, LogService logService) {
        this.billboardService = billboardService;
        this.logService = logService;
    }

    public List<BillboardChange> listAll(Long billboardId, String changeType) {
        List<BillboardChange> rows = list(Wrappers.<BillboardChange>lambdaQuery()
                .eq(billboardId != null, BillboardChange::getBillboardId, billboardId)
                .eq(StringUtils.hasText(changeType), BillboardChange::getChangeType, changeType)
                .orderByDesc(BillboardChange::getChangeDate)
                .orderByDesc(BillboardChange::getId));
        for (BillboardChange row : rows) {
            Billboard billboard = billboardService.getById(row.getBillboardId());
            if (billboard != null) {
                row.setBillboardCode(billboard.getCode());
                billboardService.fillExtra(List.of(billboard));
                row.setSectionName(billboard.getSectionName());
            }
        }
        return rows;
    }

    public BillboardChange create(BillboardChange row) {
        if (row.getBillboardId() == null || billboardService.getById(row.getBillboardId()) == null) {
            throw new BizException("请选择有效的广告牌");
        }
        if (!StringUtils.hasText(row.getChangeType())) {
            throw new BizException("请选择变动类型");
        }
        if (row.getChangeDate() == null) {
            row.setChangeDate(LocalDate.now());
        }
        row.setOperator(SecurityUtil.name());
        save(row);
        Billboard billboard = billboardService.getById(row.getBillboardId());
        logService.add("变动登记", "登记广告牌 " + billboard.getCode() + " " + row.getChangeType());
        return row;
    }
}

