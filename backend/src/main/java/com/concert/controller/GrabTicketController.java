package com.concert.controller;

import com.concert.common.Result;
import com.concert.dto.GrabTicketRequest;
import com.concert.entity.Order;
import com.concert.service.GrabTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grab")
@RequiredArgsConstructor
public class GrabTicketController {

    private final GrabTicketService grabTicketService;

    @PostMapping
    public Result<Order> grab(@Valid @RequestBody GrabTicketRequest req, HttpServletRequest httpRequest) {
        return Result.ok(grabTicketService.grabTicket(req.getTicketTierId(), req.getQuantity(), httpRequest));
    }
}
