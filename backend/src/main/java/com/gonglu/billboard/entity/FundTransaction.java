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

/**
 * 资金流水（实收实付账）：与应收账分开记录
 */
@Data
@TableName("biz_transaction")
public class FundTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate txDate;

    /** 收入 / 支出 */
    private String direction;

    /** 类别（字典 txCategory）：合同款/履约保证金/维护金/路政费/保险费/维护费 */
    private String category;

    private BigDecimal amount;

    /** 相关商家（可空 = 挂账） */
    private Long merchantId;

    /** 相关合同（可空） */
    private Long contractId;

    private String summary;

    private String operator;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String contractCode;
}
