package com.maiu.erp.modules.identity.infrastructure.persistence.order;

import com.maiu.erp.modules.identity.domain.model.Order;



public class OrderMapper {

    public static Order toDomain(OrderEntity entity) {
        Order order = new Order();
        order.setId(entity.getId());
        order.setTenantId(entity.getTenantId());
        return order;
    }

    public static OrderEntity toEntity(Order domain) {
        OrderEntity entity = new OrderEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        return entity;
    }
}
