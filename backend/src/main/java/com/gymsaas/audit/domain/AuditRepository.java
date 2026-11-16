package com.gymsaas.audit.domain;

import com.gymsaas.shared.domain.PageResult;

import java.util.UUID;

public interface AuditRepository {

    void append(AuditLog log);

    PageResult<AuditLog> findByTenant(UUID tenantId, int page, int size);
}
