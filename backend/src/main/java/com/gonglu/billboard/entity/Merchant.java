package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String contact;

    private String phone;

    private String address;

    private String taxNo;

    private String bankName;

    private String bankAccount;

    private Boolean blacklisted;

    private String remark;

    private LocalDateTime createdAt;

    /** 欠款合计（报表用，非表字段） */
    @TableField(exist = false)
    private BigDecimal arrears;
}
