package com.maiu.erp.modules.auth.application.service;

import java.util.List;

import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.auth.domain.model.Order;
import com.maiu.erp.modules.auth.domain.repository.OrderRepository;
import com.maiu.erp.modules.auth.infrastructure.security.CustomUserDetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class OrderService {

        private final OrderRepository orderRepository;

        @PersistenceContext
        private EntityManager entityManager;

        public OrderService(OrderRepository orderRepository) {
                this.orderRepository = orderRepository;
        }

        @Transactional
        public List<Order> getAll() {

                Session session = entityManager.unwrap(Session.class);

                CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();
                System.out.println("CURRENT TENANT: " + user.getTenantId());
                session.enableFilter("tenantFilter")
                                .setParameter("tenantId", user.getTenantId());

                return orderRepository.findAll();
        }

        @Transactional
        public Order getById(Long id) {

                Session session = entityManager.unwrap(Session.class);

                CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();

                session.enableFilter("tenantFilter")
                                .setParameter("tenantId", user.getTenantId());

                return orderRepository.findById(id)
                                .orElseThrow();
        }
}