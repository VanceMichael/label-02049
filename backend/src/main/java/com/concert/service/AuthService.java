package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.BusinessException;
import com.concert.common.Constants;
import com.concert.dto.LoginRequest;
import com.concert.dto.RegisterRequest;
import com.concert.entity.User;
import com.concert.mapper.UserMapper;
import com.concert.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> register(RegisterRequest req) {
        // 检查用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.USER_NORMAL);
        user.setLoginFailCount(0);
        userMapper.insert(user);

        log.info("用户注册成功: {}", req.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("message", "注册成功！激活通知已发送，请查收。");
        return result;
    }

    public Map<String, Object> login(LoginRequest req) {
        String failKey = Constants.REDIS_LOGIN_FAIL_PREFIX + req.getUsername();
        String lockKey = Constants.REDIS_LOGIN_LOCK_PREFIX + req.getUsername();

        // 检查是否被锁定（10分钟锁定期）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
            throw new BusinessException("登录失败次数过多，请" + (ttl != null && ttl > 0 ? ttl : Constants.LOGIN_LOCK_MINUTES) + "分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 5分钟滑动窗口内计数失败次数
            Long count = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, Constants.LOGIN_WINDOW_MINUTES, TimeUnit.MINUTES);
            // 达到5次，设置10分钟锁定
            if (count != null && count >= Constants.LOGIN_MAX_FAIL) {
                redisTemplate.opsForValue().set(lockKey, "1", Constants.LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
                redisTemplate.delete(failKey);
                throw new BusinessException("登录失败次数过多，请" + Constants.LOGIN_LOCK_MINUTES + "分钟后再试");
            }
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == Constants.USER_DISABLED) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 登录成功，清除失败计数和锁定
        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
        user.setLoginFailCount(0);
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        // 记住我：签发7天有效期的长令牌
        String rememberToken = null;
        if (Boolean.TRUE.equals(req.getRememberMe())) {
            rememberToken = jwtUtil.generateToken(user.getId(), user.getRole(), 7L * 24 * 3600 * 1000);
        }
        log.info("用户登录成功: {}", req.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        if (rememberToken != null) {
            result.put("rememberToken", rememberToken);
        }
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }
}
