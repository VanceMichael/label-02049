package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long concertId;
    private Long ticketTierId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Integer status; // 0=待支付, 1=已支付, 2=已取消, 3=已退款, 4=已完成
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
