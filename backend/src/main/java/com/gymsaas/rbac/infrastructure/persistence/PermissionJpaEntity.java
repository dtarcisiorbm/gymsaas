package com.gymsaas.rbac.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionJpaEntity {

    @Id
    private UUID id;

    private String code;

    private String resource;

    private String action;

    private String description;

    protected PermissionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }
}
