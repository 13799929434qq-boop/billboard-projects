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
import java.util.Arrays;
import java.util.List;

/**
 * 广告牌档案：三个桩号分别记录，证件（路政/保险/年审）独立记录有效期
 */
@Data
@TableName("biz_billboard")
public class Billboard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Long sectionId;

    /** 左侧 / 右侧 / 中央分隔带 */
    private String side;

    /** 合同桩号 */
    private String stakeContract;

    /** 现场实际桩号 */
    private String stakeActual;

    /** 路政批复桩号 */
    private String stakeApproval;

    private String spec;

    private BigDecimal width;

    private BigDecimal height;

    /** 产权（字典 ownership） */
    private String ownership;

    /** 用地性质（字典 landNature） */
    private String landNature;

    private String builder;

    private Integer buildYear;

    /** 在租 / 空闲 / 维护中 / 停用 */
    private String status;

    private String permitNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate permitExpire;

    private String insuranceNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate insuranceExpire;

    private String annualCheckNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate annualCheckDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate annualCheckExpire;

    /** 照片路径，逗号分隔 */
    private String photos;

    private String remark;

    private LocalDateTime createdAt;

    /** 路段名称（非表字段） */
    @TableField(exist = false)
    private String sectionName;

    /** 当前承租商家（非表字段） */
    @TableField(exist = false)
    private String renterName;

    /** 当前生效合同编号（非表字段） */
    @TableField(exist = false)
    private String contractCode;

    public List<String> photoList() {
        if (photos == null || photos.isBlank()) {
            return List.of();
        }
        return Arrays.stream(photos.split(",")).filter(s -> !s.isBlank()).toList();
    }
}
