package com.concert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.common.BusinessException;
import com.concert.common.Constants;
import com.concert.dto.TicketTierRequest;
import com.concert.entity.TicketTier;
import com.concert.mapper.TicketTierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTierService {

    private final TicketTierMapper ticketTierMapper;
    private final StringRedisTemplate redisTemplate;

    public TicketTier create(TicketTierRequest req) {
        TicketTier tier = new TicketTier();
        tier.setConcertId(req.getConcertId());
        tier.setTierName(req.getTierName());
        tier.setPrice(req.getPrice());
        tier.setTotalStock(req.getTotalStock());
        tier.setAvailableStock(req.getTotalStock());
        ticketTierMapper.insert(tier);

        // 同步库存到 Redis
        syncStockToRedis(tier.getId(), tier.getAvailableStock());
        log.info("创建票档: id={}, concertId={}, tier={}", tier.getId(), req.getConcertId(), req.getTierName());
        return tier;
    }

    public TicketTier update(Long id, TicketTierRequest req) {
        TicketTier tier = ticketTierMapper.selectById(id);
        if (tier == null) throw new BusinessException("票档不存在");
        tier.setTierName(req.getTierName());
        tier.setPrice(req.getPrice());

        int soldCount = tier.getTotalStock() - tier.getAvailableStock();
        if (req.getTotalStock() < soldCount) {
            throw new BusinessException("新库存不能小于已售数量(" + soldCount + ")");
        }
        tier.setTotalStock(req.getTotalStock());
        tier.setAvailableStock(req.getTotalStock() - soldCount);
        ticketTierMapper.updateById(tier);

        syncStockToRedis(tier.getId(), tier.getAvailableStock());
        log.info("更新票档: id={}", id);
        return tier;
    }

    public void delete(Long id) {
        if (ticketTierMapper.selectById(id) == null) throw new BusinessException("票档不存在");
        ticketTierMapper.deleteById(id);
        redisTemplate.delete(Constants.REDIS_STOCK_PREFIX + id);
        log.info("删除票档: id={}", id);
    }

    public List<TicketTier> listByConcert(Long concertId) {
        return ticketTierMapper.selectList(
                new LambdaQueryWrapper<TicketTier>().eq(TicketTier::getConcertId, concertId));
    }

    public void syncStockToRedis(Long tierId, int stock) {
        redisTemplate.opsForValue().set(Constants.REDIS_STOCK_PREFIX + tierId, String.valueOf(stock));
    }
}
