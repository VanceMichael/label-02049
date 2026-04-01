package com.concert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.entity.Concert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConcertMapper extends BaseMapper<Concert> {
}
