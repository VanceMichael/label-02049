package com.concert.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.Constants;
import com.concert.entity.Concert;
import com.concert.entity.Order;
import com.concert.entity.OrderLog;
import com.concert.mapper.ConcertMapper;
import com.concert.mapper.OrderLogMapper;
import com.concert.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompleteScheduler {

    private final OrderMapper orderMapper;
    private final ConcertMapper concertMapper;
    private final OrderLogMapper orderLogMapper;

    /**
     * 每5分钟扫描一次：演出结束后，已支付订单自动标记为"已完成"
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCompleteOrders() {
        // 查找所有已结束的演唱会（showTime < now）
        List<Concert> endedConcerts = concertMapper.selectList(
                new LambdaQueryWrapper<Concert>()
                        .lt(Concert::getShowTime, LocalDateTime.now())
        );
        if (endedConcerts.isEmpty()) return;

        List<Long> concertIds = endedConcerts.stream().map(Concert::getId).collect(Collectors.toList());

        // 查找这些演唱会下所有已支付的订单
        List<Order> paidOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getConcertId, concertIds)
                        .eq(Order::getStatus, Constants.ORDER_PAID)
        );

        if (paidOrders.isEmpty()) return;

        int count = 0;
        for (Order order : paidOrders) {
            order.setStatus(Constants.ORDER_COMPLETED);
            orderMapper.updateById(order);

            OrderLog logEntry = new OrderLog();
            logEntry.setOrderId(order.getId());
            logEntry.setAction("演出结束自动完成");
            logEntry.setOperator("SYSTEM");
            orderLogMapper.insert(logEntry);
            count++;
        }

        log.info("订单自动完结任务执行完毕，共完结 {} 笔订单", count);
    }
}
