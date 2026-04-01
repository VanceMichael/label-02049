package com.concert.controller;

import com.concert.common.BusinessException;
import com.concert.common.Result;
import com.concert.dto.ConcertRequest;
import com.concert.entity.Concert;
import com.concert.service.ConcertService;
import com.concert.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/concert")
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    @PostMapping
    public Result<Concert> create(@Valid @RequestBody ConcertRequest req) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(concertService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Concert> update(@PathVariable Long id, @Valid @RequestBody ConcertRequest req) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        return Result.ok(concertService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!UserContext.isAdmin()) throw new BusinessException(403, "无权限");
        concertService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<Concert> getById(@PathVariable Long id) {
        return Result.ok(concertService.getById(id));
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean excludeOffShelf) {
        return Result.ok(concertService.list(page, size, city, status, keyword, excludeOffShelf));
    }
}
