package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.BusinessException;
import com.concert.common.Constants;
import com.concert.dto.ConcertRequest;
import com.concert.entity.Concert;
import com.concert.mapper.ConcertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertMapper concertMapper;

    public Concert create(ConcertRequest req) {
        Concert concert = new Concert();
        concert.setTitle(req.getTitle());
        concert.setDescription(req.getDescription());
        concert.setArtist(req.getArtist());
        concert.setVenue(req.getVenue());
        concert.setCity(req.getCity());
        concert.setShowTime(req.getShowTime());
        concert.setPosterUrl(req.getPosterUrl());
        concert.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        concertMapper.insert(concert);
        log.info("创建演唱会: id={}, title={}", concert.getId(), concert.getTitle());
        return concert;
    }

    public Concert update(Long id, ConcertRequest req) {
        Concert concert = concertMapper.selectById(id);
        if (concert == null) throw new BusinessException("演唱会不存在");
        concert.setTitle(req.getTitle());
        concert.setDescription(req.getDescription());
        concert.setArtist(req.getArtist());
        concert.setVenue(req.getVenue());
        concert.setCity(req.getCity());
        concert.setShowTime(req.getShowTime());
        if (req.getPosterUrl() != null) concert.setPosterUrl(req.getPosterUrl());
        if (req.getStatus() != null) concert.setStatus(req.getStatus());
        concertMapper.updateById(concert);
        log.info("更新演唱会: id={}", id);
        return concert;
    }

    public void delete(Long id) {
        if (concertMapper.selectById(id) == null) throw new BusinessException("演唱会不存在");
        concertMapper.deleteById(id);
        log.info("删除演唱会: id={}", id);
    }

    public Concert getById(Long id) {
        Concert concert = concertMapper.selectById(id);
        if (concert == null) throw new BusinessException("演唱会不存在");
        return concert;
    }

    public Map<String, Object> list(int page, int size, String city, Integer status, String keyword, Boolean excludeOffShelf) {
        Page<Concert> p = new Page<>(page, size);
        LambdaQueryWrapper<Concert> wrapper = new LambdaQueryWrapper<>();
        if (city != null && !city.isBlank()) wrapper.eq(Concert::getCity, city);
        if (status != null) wrapper.eq(Concert::getStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.like(Concert::getTitle, keyword);
        if (Boolean.TRUE.equals(excludeOffShelf)) wrapper.ne(Concert::getStatus, Constants.CONCERT_OFF_SHELF);
        wrapper.orderByDesc(Concert::getShowTime);
        Page<Concert> result = concertMapper.selectPage(p, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("pages", result.getPages());
        return map;
    }
}
