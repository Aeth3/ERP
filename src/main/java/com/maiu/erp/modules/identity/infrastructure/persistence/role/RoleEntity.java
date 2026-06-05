package com.maiu.erp.modules.identity.infrastructure.persistence.role;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    @ElementCollection
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new LinkedHashSet<>();

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permissions);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        if (permissions == null) {
            permissions = new LinkedHashSet<>();
        }
        return permissions;
    }
}
