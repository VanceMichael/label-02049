package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_tier")
public class TicketTier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long concertId;
    private String tierName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
