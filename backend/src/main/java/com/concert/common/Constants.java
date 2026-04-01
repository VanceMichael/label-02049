package com.concert.common;

public final class Constants {
    private Constants() {}

    // 用户角色
    public static final int ROLE_USER = 0;
    public static final int ROLE_ADMIN = 1;

    // 用户状态
    public static final int USER_NORMAL = 0;
    public static final int USER_DISABLED = 1;

    // 演唱会状态
    public static final int CONCERT_NOT_STARTED = 0;
    public static final int CONCERT_ON_SALE = 1;
    public static final int CONCERT_ENDED = 2;
    public static final int CONCERT_OFF_SHELF = 3;

    // 订单状态
    public static final int ORDER_PENDING = 0;
    public static final int ORDER_PAID = 1;
    public static final int ORDER_CANCELLED = 2;
    public static final int ORDER_REFUNDED = 3;
    public static final int ORDER_COMPLETED = 4;

    // Redis Key 前缀
    public static final String REDIS_STOCK_PREFIX = "ticket:stock:";
    public static final String REDIS_RATE_LIMIT_PREFIX = "rate:grab:";
    public static final String REDIS_RATE_LIMIT_IP_PREFIX = "rate:grab:ip:";
    public static final String REDIS_LOGIN_FAIL_PREFIX = "login:fail:";

    // 登录限流
    public static final int LOGIN_MAX_FAIL = 5;
    public static final int LOGIN_WINDOW_MINUTES = 5;  // 5分钟窗口统计失败次数
    public static final int LOGIN_LOCK_MINUTES = 10;   // 锁定10分钟

    // 抢票限流（令牌桶）
    public static final int GRAB_RATE_LIMIT = 10; // 每分钟
    public static final int GRAB_BUCKET_CAPACITY = 10; // 桶容量

    // Redis Key 前缀 - 登录锁定
    public static final String REDIS_LOGIN_LOCK_PREFIX = "login:lock:";
    public static final int ORDER_EXPIRE_MINUTES = 15;

    // RabbitMQ
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "order.dead.queue";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";
}
