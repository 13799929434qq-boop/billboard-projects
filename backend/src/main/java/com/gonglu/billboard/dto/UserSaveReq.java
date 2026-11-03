package com.gonglu.billboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserSaveReq {

    /** 编辑时传 */
    private Long id;

    @NotBlank(message = "不能为空")
    private String username;

    /** 新建必填；编辑留空表示不修改密码 */
    private String password;

    @NotBlank(message = "不能为空")
    private String name;

    @NotBlank(message = "不能为空")
    private String role;

    private String status;
}
