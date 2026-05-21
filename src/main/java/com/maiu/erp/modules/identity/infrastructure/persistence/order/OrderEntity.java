package com.maiu.erp.modules.identity.infrastructure.persistence.order;

import org.hibernate.annotations.*;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class OrderEntity {

    @Id
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;
    @Column(name = "name")
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
    // other fields...
}
