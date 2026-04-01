package com.concert.controller;

import com.concert.common.BusinessException;
import com.concert.common.Result;
import com.concert.dto.TicketTierRequest;
import com.concert.entity.TicketTier;
import com.concert.service.TicketTierService;
import com.concert.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-tier")
@RequiredArgsConstructor
public class TicketTierController {

    private final TicketTierService ticketTierService;

    @PostMapping
    public Result<TicketTier> create(@Valid @RequestBody TicketTierRequest req) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(ticketTierService.create(req));
    }

    @PutMapping("/{id}")
    public Result<TicketTier> update(@PathVariable Long id, @Valid @RequestBody TicketTierRequest req) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(ticketTierService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        ticketTierService.delete(id);
        return Result.ok();
    }

    @GetMapping("/concert/{concertId}")
    public Result<List<TicketTier>> listByConcert(@PathVariable Long concertId) {
        return Result.ok(ticketTierService.listByConcert(concertId));
    }
}
