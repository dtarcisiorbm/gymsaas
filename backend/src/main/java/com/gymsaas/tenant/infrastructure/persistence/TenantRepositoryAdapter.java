package com.gymsaas.tenant.infrastructure.persistence;

import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.tenant.domain.Tenant;
import com.gymsaas.tenant.domain.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository repository;

    public TenantRepositoryAdapter(TenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity entity = repository.findById(tenant.id())
                .orElseGet(() -> new TenantJpaEntity(tenant.id(), tenant.name(), tenant.legalName(),
                        tenant.document(), tenant.email(), tenant.phone(), tenant.status(),
                        tenant.createdAt(), tenant.updatedAt()));
        entity.setName(tenant.name());
        entity.setLegalName(tenant.legalName());
        entity.setDocument(tenant.document());
        entity.setEmail(tenant.email());
        entity.setPhone(tenant.phone());
        entity.setStatus(tenant.status());
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsByDocument(String document, UUID excludeId) {
        if (excludeId == null) {
            return repository.findByDocument(document).isPresent();
        }
        return repository.existsByDocumentAndIdNot(document, excludeId);
    }

    @Override
    public PageResult<Tenant> search(String status, String search, int page, int size) {
        Page<TenantJpaEntity> result = repository.search(
                emptyIfBlank(status), emptyIfBlank(search), PageRequest.of(page, size));
        return PageResult.of(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    private String emptyIfBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private Tenant toDomain(TenantJpaEntity entity) {
        return new Tenant(entity.getId(), entity.getName(), entity.getLegalName(),
                entity.getDocument(), entity.getEmail(), entity.getPhone(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
