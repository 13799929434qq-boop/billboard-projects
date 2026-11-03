package com.gonglu.billboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {

    @NotBlank(message = "不能为空")
    private String username;

    @NotBlank(message = "不能为空")
    private String password;
}
