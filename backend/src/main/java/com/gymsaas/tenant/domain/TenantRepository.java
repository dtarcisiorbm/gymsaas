package com.gymsaas.tenant.domain;

import com.gymsaas.shared.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {

    Optional<Tenant> findById(UUID id);

    Tenant save(Tenant tenant);

    boolean existsByDocument(String document, UUID excludeId);

    PageResult<Tenant> search(String status, String search, int page, int size);
}
