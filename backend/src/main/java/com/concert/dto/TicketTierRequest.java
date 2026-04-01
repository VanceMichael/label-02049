package com.concert.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketTierRequest {
    @NotNull(message = "演唱会ID不能为空")
    private Long concertId;
    @NotBlank(message = "票档名称不能为空")
    private String tierName;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存至少为1")
    private Integer totalStock;
}
