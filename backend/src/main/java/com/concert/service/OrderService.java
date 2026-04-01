package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.BusinessException;
import com.concert.common.Constants;
import com.concert.entity.Order;
import com.concert.entity.OrderLog;
import com.concert.mapper.OrderLogMapper;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.TicketTierMapper;
import com.concert.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final TicketTierMapper ticketTierMapper;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    public Map<String, Object> myOrders(int page, int size) {
        Page<Order> p = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, UserContext.getUserId())
                .orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(p, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return map;
    }

    public Map<String, Object> listOrders(int page, int size, String orderNo,
                                           Long userId, Long concertId, Integer status) {
        Page<Order> p = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) wrapper.like(Order::getOrderNo, orderNo);
        if (userId != null) wrapper.eq(Order::getUserId, userId);
        if (concertId != null) wrapper.eq(Order::getConcertId, concertId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(p, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return map;
    }

    @Transactional
    public void pay(Long orderId) {
        Order order = getOrderForCurrentUser(orderId);
        if (order.getStatus() != Constants.ORDER_PENDING) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setStatus(Constants.ORDER_PAID);
        orderMapper.updateById(order);
        addLog(orderId, "支付", UserContext.getUserId().toString());

        notificationService.sendPersonalNotification(order.getUserId(), "支付成功",
                "订单 " + order.getOrderNo() + " 已支付成功，祝您观演愉快！");
        log.info("订单支付: orderId={}", orderId);
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = getOrderForCurrentUser(orderId);
        if (order.getStatus() != Constants.ORDER_PENDING) {
            throw new BusinessException("仅待支付订单可取消");
        }
        order.setStatus(Constants.ORDER_CANCELLED);
        orderMapper.updateById(order);
        releaseStock(order);
        addLog(orderId, "取消", UserContext.getUserId().toString());

        notificationService.sendPersonalNotification(order.getUserId(), "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消，库存已释放。");
        log.info("订单取消: orderId={}", orderId);
    }

    @Transactional
    public void refund(Long orderId) {
        Order order = getOrderForCurrentUser(orderId);
        if (order.getStatus() != Constants.ORDER_PAID) {
            throw new BusinessException("仅已支付订单可退款");
        }
        order.setStatus(Constants.ORDER_REFUNDED);
        orderMapper.updateById(order);
        releaseStock(order);
        addLog(orderId, "退款", UserContext.getUserId().toString());

        notificationService.sendPersonalNotification(order.getUserId(), "退款成功",
                "订单 " + order.getOrderNo() + " 已退款，库存已释放。");
        log.info("订单退款: orderId={}", orderId);
    }

    @Transactional
    public void approve(Long orderId) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != Constants.ORDER_PAID) {
            throw new BusinessException("仅已支付订单可审核通过");
        }
        order.setStatus(Constants.ORDER_COMPLETED);
        orderMapper.updateById(order);
        addLog(orderId, "管理员审核通过", UserContext.getUserId().toString());

        notificationService.sendPersonalNotification(order.getUserId(), "订单审核通过",
                "订单 " + order.getOrderNo() + " 已审核通过，祝您观演愉快！");
        log.info("管理员审核通过订单: orderId={}", orderId);
    }

    @Transactional
    public void reject(Long orderId) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getStatus() != Constants.ORDER_PAID) {
            throw new BusinessException("仅已支付订单可审核拒绝");
        }
        order.setStatus(Constants.ORDER_REFUNDED);
        orderMapper.updateById(order);
        releaseStock(order);
        addLog(orderId, "管理员审核拒绝", UserContext.getUserId().toString());

        notificationService.sendPersonalNotification(order.getUserId(), "订单审核拒绝",
                "订单 " + order.getOrderNo() + " 审核未通过，已自动退款。");
        log.info("管理员审核拒绝订单: orderId={}", orderId);
    }

    @Transactional
    public void expireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Constants.ORDER_PENDING) return;
        order.setStatus(Constants.ORDER_CANCELLED);
        orderMapper.updateById(order);
        releaseStock(order);
        addLog(orderId, "超时自动取消", "SYSTEM");

        notificationService.sendPersonalNotification(order.getUserId(), "订单已超时取消",
                "订单 " + order.getOrderNo() + " 因超时未支付已自动取消。");
        log.info("订单超时取消: orderId={}", orderId);
    }

    private Order getOrderForCurrentUser(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (!UserContext.isAdmin() && !order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作此订单");
        }
        return order;
    }

    private void releaseStock(Order order) {
        // Redis 库存恢复
        String stockKey = Constants.REDIS_STOCK_PREFIX + order.getTicketTierId();
        redisTemplate.opsForValue().increment(stockKey, order.getQuantity());
        // MySQL 库存恢复
        ticketTierMapper.restoreStock(order.getTicketTierId(), order.getQuantity());
        log.info("库存释放: ticketTierId={}, quantity={}", order.getTicketTierId(), order.getQuantity());
    }

    private void addLog(Long orderId, String action, String operator) {
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setAction(action);
        logEntry.setOperator(operator);
        orderLogMapper.insert(logEntry);
    }
}
