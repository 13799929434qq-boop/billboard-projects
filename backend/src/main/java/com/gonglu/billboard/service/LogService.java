package com.gonglu.billboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gonglu.billboard.entity.SysOperationLog;
import com.gonglu.billboard.mapper.SysOperationLogMapper;
import com.gonglu.billboard.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService extends ServiceImpl<SysOperationLogMapper, SysOperationLog> {

    /** 记录操作日志（谁、什么时候、改了什么） */
    public void add(String action, String detail) {
        SysOperationLog log = new SysOperationLog();
        log.setTime(LocalDateTime.now());
        log.setUsername(SecurityUtil.username());
        log.setName(SecurityUtil.name());
        log.setAction(action);
        log.setDetail(detail);
        save(log);
    }

    /** 登录等无上下文场景使用 */
    public void add(String username, String name, String action, String detail) {
        SysOperationLog log = new SysOperationLog();
        log.setTime(LocalDateTime.now());
        log.setUsername(username);
        log.setName(name);
        log.setAction(action);
        log.setDetail(detail);
        save(log);
    }

    public Page<SysOperationLog> pageQuery(long page, long size, String username) {
        return page(new Page<>(page, size),
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysOperationLog>lambdaQuery()
                        .like(username != null && !username.isBlank(), SysOperationLog::getUsername, username)
                        .orderByDesc(SysOperationLog::getTime));
    }
}
