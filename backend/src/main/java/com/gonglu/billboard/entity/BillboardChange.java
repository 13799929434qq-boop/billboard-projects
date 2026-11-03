package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_billboard_change")
public class BillboardChange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long billboardId;
    private String changeType;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate changeDate;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private String operator;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String billboardCode;
    @TableField(exist = false)
    private String sectionName;
}

