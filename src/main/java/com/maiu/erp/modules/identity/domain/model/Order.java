package com.maiu.erp.modules.identity.domain.model;

public class Order {

    private Long id;
    private Long tenantId;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }
    // other fields

    // getters/setters
}