package com.gonglu.billboard.controller;

import com.gonglu.billboard.common.R;
import com.gonglu.billboard.dto.UserSaveReq;
import com.gonglu.billboard.entity.SysUser;
import com.gonglu.billboard.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "账号管理（仅管理员）")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "账号列表")
    @GetMapping
    public R<List<SysUser>> list() {
        return R.ok(userService.listAll());
    }

    @Operation(summary = "新增账号")
    @PostMapping
    public R<SysUser> create(@Valid @RequestBody UserSaveReq req) {
        return R.ok(userService.create(req));
    }

    @Operation(summary = "修改账号")
    @PutMapping("/{id}")
    public R<SysUser> update(@PathVariable Long id, @Valid @RequestBody UserSaveReq req) {
        req.setId(id);
        return R.ok(userService.update(req));
    }

    @Operation(summary = "启用 / 停用账号")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return R.ok();
    }
}
