package com.gonglu.billboard.security;

/**
 * 当前登录用户（写入 SecurityContext 的 principal）
 */
public record CurrentUser(String username, String name, String role) {
}
