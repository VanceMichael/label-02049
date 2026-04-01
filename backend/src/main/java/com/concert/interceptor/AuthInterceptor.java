package com.concert.interceptor;

import com.concert.common.BusinessException;
import com.concert.util.JwtUtil;
import com.concert.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 公开 GET 接口放行（演唱会列表、详情）
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            if (uri.equals("/api/concert/list") || uri.matches("/api/concert/\\d+")) {
                return true;
            }
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或令牌已过期");
        }
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "令牌无效或已过期");
        }
        Claims claims = jwtUtil.parseToken(token);
        UserContext.setUserId(Long.parseLong(claims.getSubject()));
        UserContext.setUserRole(claims.get("role", Integer.class));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
