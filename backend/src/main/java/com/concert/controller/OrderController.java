package com.concert.controller;

import com.concert.common.BusinessException;
import com.concert.common.Result;
import com.concert.service.OrderService;
import com.concert.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my")
    public Result<Map<String, Object>> myOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.myOrders(page, size));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long concertId,
            @RequestParam(required = false) Integer status) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(orderService.listOrders(page, size, orderNo, userId, concertId, status));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.ok();
    }

    @PostMapping("/{id}/refund")
    public Result<Void> refund(@PathVariable Long id) {
        orderService.refund(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        orderService.approve(id);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        orderService.reject(id);
        return Result.ok();
    }
}
