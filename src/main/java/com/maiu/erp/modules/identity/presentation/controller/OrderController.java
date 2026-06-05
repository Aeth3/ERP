package com.maiu.erp.modules.identity.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.maiu.erp.modules.identity.application.service.OrderService;

import java.util.List;

import com.maiu.erp.modules.identity.domain.model.Order;


@RestController
@RequestMapping("/orders")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAll();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }
}
