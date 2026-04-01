package com.concert.listener;

import com.concert.common.Constants;
import com.concert.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireListener {

    private final OrderService orderService;

    @RabbitListener(queues = Constants.ORDER_DEAD_QUEUE)
    public void handleExpiredOrder(String message) {
        try {
            Long orderId = Long.parseLong(message);
            log.info("收到订单超时消息: orderId={}", orderId);
            orderService.expireOrder(orderId);
        } catch (Exception e) {
            log.error("处理订单超时消息失败: {}", message, e);
        }
    }
}
