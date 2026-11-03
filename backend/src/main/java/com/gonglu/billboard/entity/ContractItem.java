package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同明细：某块牌在该合同下的每年租金
 */
@Data
@TableName("biz_contract_item")
public class ContractItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private Long billboardId;

    private BigDecimal yearlyPrice;

    /** 广告牌编号（非表字段） */
    @TableField(exist = false)
    private String billboardCode;

    /** 广告牌位置描述（非表字段） */
    @TableField(exist = false)
    private String billboardLocation;
}
