package com.gymsaas.audit.infrastructure.persistence;

import com.gymsaas.audit.domain.AuditLog;
import com.gymsaas.audit.domain.AuditRepository;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
public class AuditRepositoryAdapter implements AuditRepository {

    private final AuditLogJpaRepository repository;

    public AuditRepositoryAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(AuditLog log) {
        repository.save(new AuditLogJpaEntity(log.id(), log.tenantId(), log.actorType(),
                log.actorId(), log.action(), log.resourceType(), log.resourceId(),
                log.oldValues(), log.newValues(), log.metadata() == null ? Map.of() : log.metadata(),
                log.riskLevel(), log.occurredAt()));
    }

    @Override
    public PageResult<AuditLog> findByTenant(UUID tenantId, int page, int size) {
        Page<AuditLogJpaEntity> result = repository.findByTenantId(tenantId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return PageResult.of(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    private AuditLog toDomain(AuditLogJpaEntity entity) {
        return new AuditLog(entity.getId(), entity.getTenantId(), entity.getActorType(),
                entity.getActorId(), entity.getAction(), entity.getResourceType(),
                entity.getResourceId(), entity.getOldValues(), entity.getNewValues(),
                entity.getMetadata(), entity.getRiskLevel(), entity.getOccurredAt());
    }
}
