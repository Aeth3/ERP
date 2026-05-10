package com.maiu.erp.modules.auth.domain.repository;

import java.util.List;
import java.util.Optional;

import com.maiu.erp.modules.auth.domain.model.Order;


public interface OrderRepository {
    List<Order> findAll();

    Optional<Order> findById(Long id);

    Order save(Order order);

}
