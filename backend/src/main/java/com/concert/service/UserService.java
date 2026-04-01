package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.BusinessException;
import com.concert.dto.ProfileUpdateRequest;
import com.concert.entity.User;
import com.concert.mapper.UserMapper;
import com.concert.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User getProfile() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(null);
        return user;
    }

    public void updateProfile(ProfileUpdateRequest req) {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) throw new BusinessException("用户不存在");

        if (req.getUsername() != null) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, req.getUsername())
                            .ne(User::getId, user.getId()));
            if (count > 0) throw new BusinessException("用户名已存在");
            user.setUsername(req.getUsername());
        }
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        userMapper.updateById(user);
        log.info("用户信息更新: userId={}", user.getId());
    }

    public Map<String, Object> listUsers(int page, int size, String keyword) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(p, wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return map;
    }

    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("管理员修改用户状态: userId={}, status={}", userId, status);
    }
}
