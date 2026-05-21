package com.maiu.erp.modules.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.maiu.erp.modules.identity.domain.model.Order;


public interface OrderRepository {
    List<Order> findAll();

    Optional<Order> findById(Long id);

    Order save(Order order);

}
