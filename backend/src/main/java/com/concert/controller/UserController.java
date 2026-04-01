package com.concert.controller;

import com.concert.common.BusinessException;
import com.concert.common.Result;
import com.concert.dto.ProfileUpdateRequest;
import com.concert.entity.User;
import com.concert.service.UserService;
import com.concert.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<User> getProfile() {
        return Result.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest req) {
        userService.updateProfile(req);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(userService.listUsers(page, size, keyword));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        userService.updateUserStatus(id, status);
        return Result.ok();
    }
}
