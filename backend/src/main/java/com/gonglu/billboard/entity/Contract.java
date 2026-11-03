package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 租赁合同：一份合同绑定多块广告牌（见 ContractItem）
 */
@Data
@TableName("biz_contract")
public class Contract {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Long merchantId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate signDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer years;

    private BigDecimal totalAmount;

    /** 草稿 / 生效 / 解除（到期为虚拟状态，按 endDate 推算） */
    private String status;

    private String remark;

    private LocalDateTime createdAt;

    /** 合同明细（非表字段） */
    @TableField(exist = false)
    private List<ContractItem> items;

    @TableField(exist = false)
    private String merchantName;

    /** 已收合计（非表字段） */
    @TableField(exist = false)
    private BigDecimal receivedAmount;

    /** 剩余天数（非表字段，负数表示已过期） */
    @TableField(exist = false)
    private Long daysRemaining;

    /** 展示用状态：生效且已过期 -> 到期 */
    public String getDisplayStatus() {
        if ("生效".equals(status) && endDate != null && endDate.isBefore(LocalDate.now())) {
            return "到期";
        }
        return status;
    }

    public void computeDerived() {
        if (endDate != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        }
    }
}
