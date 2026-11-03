package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.common.BizException;
import com.gonglu.billboard.dto.UserSaveReq;
import com.gonglu.billboard.entity.SysUser;
import com.gonglu.billboard.mapper.SysUserMapper;
import com.gonglu.billboard.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final LogService logService;

    public UserService(LogService logService) {
        this.logService = logService;
    }

    public List<SysUser> listAll() {
        return list(Wrappers.<SysUser>lambdaQuery().orderByAsc(SysUser::getId));
    }

    public SysUser create(UserSaveReq req) {
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BizException("新建账号必须设置密码");
        }
        Long exists = count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));
        if (exists > 0) {
            throw new BizException("账号 " + req.getUsername() + " 已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        // 演示环境 {noop} 明文；生产请改 BCrypt：new BCryptPasswordEncoder().encode(pwd)
        user.setPassword("{noop}" + req.getPassword());
        user.setName(req.getName());
        user.setRole(req.getRole());
        user.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "启用");
        user.setCreatedAt(LocalDateTime.now());
        save(user);
        logService.add("新增", "新增账号 " + req.getUsername() + "（" + roleName(req.getRole()) + "）");
        return user;
    }

    public SysUser update(UserSaveReq req) {
        SysUser user = getById(req.getId());
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (!user.getUsername().equals(req.getUsername())) {
            Long exists = count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));
            if (exists > 0) {
                throw new BizException("账号 " + req.getUsername() + " 已存在");
            }
            user.setUsername(req.getUsername());
        }
        user.setName(req.getName());
        user.setRole(req.getRole());
        if (StringUtils.hasText(req.getStatus())) {
            user.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword("{noop}" + req.getPassword());
        }
        updateById(user);
        logService.add("修改", "修改账号 " + user.getUsername());
        return user;
    }

    public void toggleStatus(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (user.getUsername().equals(SecurityUtil.username())) {
            throw new BizException("不能停用自己的账号");
        }
        user.setStatus(user.isEnabled() ? "停用" : "启用");
        updateById(user);
        logService.add("修改", (user.isEnabled() ? "启用" : "停用") + "账号 " + user.getUsername());
    }

    private String roleName(String role) {
        return switch (role) {
            case "admin" -> "系统管理员";
            case "archivist" -> "运营/档案员";
            case "finance" -> "合同/财务";
            case "leader" -> "领导";
            default -> role;
        };
    }
}
