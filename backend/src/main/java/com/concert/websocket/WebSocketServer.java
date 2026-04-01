package com.concert.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    private static final ConcurrentHashMap<Long, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        SESSIONS.put(userId, session);
        log.info("WebSocket连接建立: userId={}", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        SESSIONS.remove(userId);
        log.info("WebSocket连接关闭: userId={}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误", error);
    }

    public void sendToUser(Long userId, String message) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("WebSocket发送消息失败: userId={}", userId, e);
            }
        }
    }

    public void broadcast(String message) {
        SESSIONS.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("WebSocket广播失败", e);
                }
            }
        });
    }
}
