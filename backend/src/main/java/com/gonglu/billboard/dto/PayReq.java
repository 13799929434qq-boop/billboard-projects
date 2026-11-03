package com.gonglu.billboard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收款登记请求（支持部分缴纳）
 */
@Data
public class PayReq {

    @NotNull(message = "不能为空")
    @DecimalMin(value = "0.01", message = "必须大于 0")
    private BigDecimal amount;
}
