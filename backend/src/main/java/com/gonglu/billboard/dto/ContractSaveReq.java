package com.gonglu.billboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 合同新建/编辑请求：一份合同绑定多块广告牌，每块牌每年价格可不同
 */
@Data
public class ContractSaveReq {

    /** 编辑时传 */
    private Long id;

    /** 合同编号，留空自动生成 */
    private String code;

    @NotNull(message = "不能为空")
    private Long merchantId;

    private LocalDate signDate;

    @NotNull(message = "不能为空")
    private LocalDate startDate;

    @NotNull(message = "不能为空")
    private LocalDate endDate;

    /** 草稿 / 生效 */
    @NotNull(message = "不能为空")
    private String status;

    private String remark;

    @Valid
    @NotEmpty(message = "至少绑定一块广告牌")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "不能为空")
        private Long billboardId;
        @NotNull(message = "不能为空")
        private BigDecimal yearlyPrice;
    }
}
