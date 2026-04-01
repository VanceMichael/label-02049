package com.concert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("concert")
public class Concert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String artist;
    private String venue;
    private String city;
    private LocalDateTime showTime;
    private String posterUrl;
    private Integer status; // 0=未开始, 1=售票中, 2=已结束, 3=已下架
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
