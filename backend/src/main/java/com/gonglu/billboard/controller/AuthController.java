package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.dto.LoginReq;
import com.gonglu.billboard.security.CurrentUser;
import com.gonglu.billboard.security.SecurityUtil;
import com.gonglu.billboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "登录认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        return R.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<CurrentUser> me() {
        return R.ok(SecurityUtil.current());
    }

    @Operation(summary = "退出登录（前端清除 token 即可）")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
