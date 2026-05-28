package com.exchange.gateway.controller;

import com.exchange.common.command.PlaceOrderCommand;
import com.exchange.gateway.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<PlaceOrderCommand> placeOrder(
            @RequestBody PlaceOrderCommand command,
            @AuthenticationPrincipal Long userId) {
        command.setUserId(userId);
        return orderService.placeOrder(command);
    }
}
