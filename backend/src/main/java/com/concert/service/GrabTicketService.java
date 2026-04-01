package com.concert.service;

import com.concert.common.BusinessException;
import com.concert.common.Constants;
import com.concert.entity.Concert;
import com.concert.entity.Order;
import com.concert.entity.OrderLog;
import com.concert.entity.TicketTier;
import com.concert.entity.User;
import com.concert.mapper.ConcertMapper;
import com.concert.mapper.OrderLogMapper;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.TicketTierMapper;
import com.concert.mapper.UserMapper;
import com.concert.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrabTicketService {

    private final StringRedisTemplate redisTemplate;
    private final TicketTierMapper ticketTierMapper;
    private final ConcertMapper concertMapper;
    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    // Redis Lua 脚本：原子扣减库存
    private static final String DEDUCT_STOCK_LUA =
            "local stock = tonumber(redis.call('get', KEYS[1])) " +
            "if stock == nil then return -1 end " +
            "if stock < tonumber(ARGV[1]) then return 0 end " +
            "redis.call('decrby', KEYS[1], ARGV[1]) " +
            "return 1";

    // Redis Lua 脚本：令牌桶限流
    private static final String TOKEN_BUCKET_LUA =
            "local key = KEYS[1] " +
            "local capacity = tonumber(ARGV[1]) " +
            "local refillRate = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local requested = 1 " +
            "local data = redis.call('hmget', key, 'tokens', 'lastRefill') " +
            "local tokens = tonumber(data[1]) " +
            "local lastRefill = tonumber(data[2]) " +
            "if tokens == nil then " +
            "  tokens = capacity " +
            "  lastRefill = now " +
            "end " +
            "local elapsed = math.max(0, now - lastRefill) " +
            "local newTokens = math.min(capacity, tokens + elapsed * refillRate / 60) " +
            "if newTokens < requested then " +
            "  redis.call('hmset', key, 'tokens', newTokens, 'lastRefill', now) " +
            "  redis.call('expire', key, 120) " +
            "  return 0 " +
            "end " +
            "newTokens = newTokens - requested " +
            "redis.call('hmset', key, 'tokens', newTokens, 'lastRefill', now) " +
            "redis.call('expire', key, 120) " +
            "return 1";

    public Order grabTicket(Long ticketTierId, int quantity, HttpServletRequest httpRequest) {
        Long userId = UserContext.getUserId();
        String clientIp = getClientIp(httpRequest);

        // 0. 检查用户状态（禁用用户不可抢票）
        checkUserStatus(userId);

        // 1. 令牌桶限流检查（用户维度 + IP维度）
        checkTokenBucketRateLimit(Constants.REDIS_RATE_LIMIT_PREFIX + userId);
        checkTokenBucketRateLimit(Constants.REDIS_RATE_LIMIT_IP_PREFIX + clientIp);

        // 2. 校验票档和演唱会状态
        TicketTier tier = ticketTierMapper.selectById(ticketTierId);
        if (tier == null) throw new BusinessException("票档不存在");

        Concert concert = concertMapper.selectById(tier.getConcertId());
        if (concert == null) throw new BusinessException("演唱会不存在");
        if (concert.getStatus() != Constants.CONCERT_ON_SALE) {
            throw new BusinessException("该演唱会当前不可购票");
        }

        // 3. Redis 库存初始化检查（若不存在则从 MySQL 同步）
        String stockKey = Constants.REDIS_STOCK_PREFIX + ticketTierId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
            redisTemplate.opsForValue().set(stockKey, String.valueOf(tier.getAvailableStock()));
            log.info("Redis 库存初始化: ticketTierId={}, stock={}", ticketTierId, tier.getAvailableStock());
        }

        // 4. Redis 原子扣减库存
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(DEDUCT_STOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey), String.valueOf(quantity));

        if (result == null || result == -1) {
            throw new BusinessException("库存信息异常");
        }
        if (result == 0) {
            throw new BusinessException("票已售罄，抢票失败");
        }

        // 5. 创建订单（Redis扣减成功后立即创建）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setConcertId(concert.getId());
        order.setTicketTierId(ticketTierId);
        order.setQuantity(quantity);
        order.setTotalPrice(tier.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(Constants.ORDER_PENDING);
        order.setExpireAt(LocalDateTime.now().plusMinutes(Constants.ORDER_EXPIRE_MINUTES));
        orderMapper.insert(order);

        // 6. 异步校验 MySQL 库存（不一致则回滚）
        asyncVerifyMySQLStock(order.getId(), ticketTierId, quantity, stockKey);

        // 7. 发送延迟消息（15分钟后检查支付状态）
        rabbitTemplate.convertAndSend(
                Constants.ORDER_DELAY_EXCHANGE,
                Constants.ORDER_DELAY_ROUTING_KEY,
                String.valueOf(order.getId()));

        // 8. 发送通知
        notificationService.sendPersonalNotification(userId, "抢票成功",
                "恭喜！您已成功抢到 [" + concert.getTitle() + "] " + tier.getTierName() +
                " x" + quantity + "，请在15分钟内完成支付。");

        log.info("抢票成功: userId={}, orderId={}, ticketTierId={}, quantity={}, ip={}",
                userId, order.getId(), ticketTierId, quantity, clientIp);
        return order;
    }

    /**
     * 检查用户状态（禁用用户不可抢票）
     */
    private void checkUserStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == Constants.USER_DISABLED) {
            throw new BusinessException("账号已被禁用，无法抢票");
        }
    }

    /**
     * 令牌桶限流检查
     */
    private void checkTokenBucketRateLimit(String key) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);
        long now = System.currentTimeMillis() / 1000;
        Long allowed = redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(Constants.GRAB_BUCKET_CAPACITY),
                String.valueOf(Constants.GRAB_RATE_LIMIT),
                String.valueOf(now));
        if (allowed == null || allowed == 0) {
            throw new BusinessException("操作过于频繁，请稍后再试");
        }
    }

    /**
     * 异步校验 MySQL 库存，不一致则回滚 Redis 并取消订单
     */
    @Async
    public void asyncVerifyMySQLStock(Long orderId, Long ticketTierId, int quantity, String stockKey) {
        try {
            int affected = ticketTierMapper.deductStock(ticketTierId, quantity);
            if (affected == 0) {
                // MySQL 库存不足，回滚 Redis 并取消订单
                redisTemplate.opsForValue().increment(stockKey, quantity);
                Order order = orderMapper.selectById(orderId);
                if (order != null && order.getStatus() == Constants.ORDER_PENDING) {
                    order.setStatus(Constants.ORDER_CANCELLED);
                    orderMapper.updateById(order);
                    OrderLog logEntry = new OrderLog();
                    logEntry.setOrderId(orderId);
                    logEntry.setAction("库存校验失败自动取消");
                    logEntry.setOperator("SYSTEM");
                    orderLogMapper.insert(logEntry);
                    notificationService.sendPersonalNotification(order.getUserId(), "抢票失败",
                            "很抱歉，库存校验不通过，订单已自动取消。");
                }
                log.warn("MySQL库存校验失败，已回滚: orderId={}, ticketTierId={}", orderId, ticketTierId);
            } else {
                log.info("MySQL库存校验通过: orderId={}, ticketTierId={}", orderId, ticketTierId);
            }
        } catch (Exception e) {
            // 异步校验异常，回滚 Redis
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.error("异步库存校验异常: orderId={}, ticketTierId={}", orderId, ticketTierId, e);
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
