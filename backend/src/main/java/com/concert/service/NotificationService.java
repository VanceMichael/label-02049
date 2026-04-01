package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.BusinessException;
import com.concert.entity.Notification;
import com.concert.mapper.NotificationMapper;
import com.concert.util.UserContext;
import com.concert.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final WebSocketServer webSocketServer;

    public void sendPersonalNotification(Long userId, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("PERSONAL");
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
        webSocketServer.sendToUser(userId, "{\"type\":\"notification\",\"title\":\"" + title + "\",\"content\":\"" + content + "\"}");
    }

    public void publishSystemNotification(String title, String content) {
        Notification n = new Notification();
        n.setUserId(null);
        n.setType("SYSTEM");
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
        webSocketServer.broadcast("{\"type\":\"system\",\"title\":\"" + title + "\",\"content\":\"" + content + "\"}");
        log.info("发布系统公告: {}", title);
    }

    public Map<String, Object> list(int page, int size) {
        Long userId = UserContext.getUserId();
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId))
                .orderByDesc(Notification::getCreatedAt);
        Page<Notification> result = notificationMapper.selectPage(p, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return map;
    }

    public long unreadCount() {
        Long userId = UserContext.getUserId();
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId))
                        .eq(Notification::getIsRead, 0));
    }

    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) throw new BusinessException("通知不存在");
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    public void markAllRead() {
        Long userId = UserContext.getUserId();
        notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .and(w -> w.eq(Notification::getUserId, userId).or().isNull(Notification::getUserId))
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1));
    }
}
