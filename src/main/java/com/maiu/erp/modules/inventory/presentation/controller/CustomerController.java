package com.maiu.erp.modules.inventory.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.inventory.application.dto.CreateCustomerRequest;
import com.maiu.erp.modules.inventory.application.dto.CustomerDto;
import com.maiu.erp.modules.inventory.application.dto.IdResponse;
import com.maiu.erp.modules.inventory.application.service.CustomerService;
import com.maiu.erp.modules.inventory.domain.model.Customer;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<IdResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setCode(request.code());
        customer.setName(request.name());
        customer.setContactPerson(request.contactPerson());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setActive(request.active() == null || request.active());

        UUID customerId = customerService.createCustomer(customer);
        return ResponseEntity.status(201).body(new IdResponse(customerId));
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getCustomers(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(
                customerService.getCustomers(activeOnly).stream()
                        .map(this::toDto)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomerById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                toDto(customerService.getCustomerById(id)));
    }

    private CustomerDto toDto(Customer customer) {
        return new CustomerDto(
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                customer.getContactPerson(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getActive());
    }
}
