package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 应收账单（应收账）：合同生效时按 年×牌 拆分生成
 */
@Data
@TableName("biz_receivable")
public class Receivable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;

    private Long merchantId;

    private Long billboardId;

    /** 应收年度 */
    private Integer year;

    private BigDecimal amount;

    private BigDecimal received;

    /** 未收 / 部分 / 已收 */
    private String status;

    @TableField(exist = false)
    private String contractCode;

    @TableField(exist = false)
    private String merchantName;

    @TableField(exist = false)
    private String billboardCode;

    @TableField(exist = false)
    private String sectionName;

    /** 剩余应收 */
    public BigDecimal getRemain() {
        return amount.subtract(received);
    }

    public void refreshStatus() {
        if (received.compareTo(amount) >= 0) {
            status = "已收";
        } else if (received.signum() > 0) {
            status = "部分缴纳";
        } else {
            status = "未收";
        }
    }
}
