package com.concert.controller;

import com.concert.common.BusinessException;
import com.concert.common.Result;
import com.concert.dto.NotificationRequest;
import com.concert.service.NotificationService;
import com.concert.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(notificationService.list(page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody NotificationRequest req) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        notificationService.publishSystemNotification(req.getTitle(), req.getContent());
        return Result.ok();
    }
}
