package com.gonglu.billboard.security;

import com.gonglu.billboard.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    private SecurityUtil() {
    }

    /** 获取当前登录用户，未登录抛异常 */
    public static CurrentUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser u)) {
            throw new BizException("登录状态已失效，请重新登录");
        }
        return u;
    }

    public static String username() {
        return current().username();
    }

    public static String name() {
        return current().name();
    }
}
