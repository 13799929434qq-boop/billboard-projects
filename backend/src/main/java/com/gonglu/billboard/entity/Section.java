package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路段档案（组织层级：路段 → 地区 → 运营单位）
 */
@Data
@TableName("biz_section")
public class Section {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String region;

    private String operator;

    private String mileage;

    private LocalDateTime createdAt;

    /** 路段下广告牌数量（报表用，非表字段） */
    @TableField(exist = false)
    private Long billboardCount;
}
