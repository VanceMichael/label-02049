package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type; // SYSTEM / PERSONAL
    private String title;
    private String content;
    private Integer isRead; // 0=未读, 1=已读
    private LocalDateTime createdAt;
}
