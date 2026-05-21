package com.maiu.erp.modules.identity.infrastructure.persistence.order;

import org.springframework.stereotype.Repository;

import com.maiu.erp.modules.identity.domain.repository.OrderRepository;

import java.util.List;
import java.util.Optional;

import com.maiu.erp.modules.identity.domain.model.Order;



@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(OrderMapper::toDomain) // 🔥 important
                .toList();
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderMapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return OrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id)
                .map(OrderMapper::toDomain);
    }
}