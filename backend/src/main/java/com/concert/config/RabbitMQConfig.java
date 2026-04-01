package com.concert.config;

import com.concert.common.Constants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // 延迟队列（TTL 15分钟后转发到死信队列）
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(Constants.ORDER_DELAY_EXCHANGE);
    }

    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", Constants.ORDER_DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", Constants.ORDER_DEAD_ROUTING_KEY);
        args.put("x-message-ttl", Constants.ORDER_EXPIRE_MINUTES * 60 * 1000);
        return new Queue(Constants.ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(Constants.ORDER_DELAY_ROUTING_KEY);
    }

    // 死信队列（实际消费队列）
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(Constants.ORDER_DEAD_EXCHANGE);
    }

    @Bean
    public Queue deadQueue() {
        return new Queue(Constants.ORDER_DEAD_QUEUE, true);
    }

    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(Constants.ORDER_DEAD_ROUTING_KEY);
    }
}
