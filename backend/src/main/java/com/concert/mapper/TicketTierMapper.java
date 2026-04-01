package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.TicketTier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketTierMapper extends BaseMapper<TicketTier> {

    @Update("UPDATE ticket_tier SET available_stock = available_stock - #{quantity} " +
            "WHERE id = #{id} AND available_stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE ticket_tier SET available_stock = available_stock + #{quantity} " +
            "WHERE id = #{id} AND available_stock + #{quantity} <= total_stock")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
