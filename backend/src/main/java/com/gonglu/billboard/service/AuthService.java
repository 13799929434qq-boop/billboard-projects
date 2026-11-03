package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.entity.SysUser;
import com.gonglu.billboard.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LogService logService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, LogService logService, LoginAttemptService loginAttemptService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.logService = logService;
        this.loginAttemptService = loginAttemptService;
    }

    public Map<String, Object> login(String username, String password) {
        // 登录防爆破：连续失败 5 次锁定 10 分钟（Redis 不可用时自动降级为不锁定）
        if (loginAttemptService.isLocked(username)) {
            throw new BizException("账号连续登录失败次数过多，已锁定 10 分钟，请稍后再试");
        }
        SysUser user = userService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            int fails = loginAttemptService.onFail(username);
            if (fails >= 3 && fails < 5) {
                throw new BizException("账号或密码错误，再失败 " + (5 - fails) + " 次将锁定 10 分钟");
            }
            throw new BizException("账号或密码错误");
        }
        if (!user.isEnabled()) {
            throw new BizException("账号已停用，请联系管理员");
        }
        loginAttemptService.onSuccess(username);
        String token = jwtUtil.generate(user.getUsername(), user.getName(), user.getRole());
        logService.add(user.getUsername(), user.getName(), "登录", "登录系统");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> u = new HashMap<>();
        u.put("username", user.getUsername());
        u.put("name", user.getName());
        u.put("role", user.getRole());
        result.put("user", u);
        return result;
    }
}
