package com.concert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConcertRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    @NotBlank(message = "艺人不能为空")
    private String artist;
    @NotBlank(message = "场馆不能为空")
    private String venue;
    @NotBlank(message = "城市不能为空")
    private String city;
    @NotNull(message = "演出时间不能为空")
    private LocalDateTime showTime;
    private String posterUrl;
    private Integer status;
}
