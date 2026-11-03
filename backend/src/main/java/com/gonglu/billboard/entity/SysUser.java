package com.gonglu.billboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 只写不读：查询返回时不下发密码 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String name;

    /** admin / archivist / finance / leader */
    private String role;

    /** 启用 / 停用 */
    private String status;

    private LocalDateTime createdAt;

    @JsonIgnore
    public boolean isEnabled() {
        return "启用".equals(status);
    }
}
