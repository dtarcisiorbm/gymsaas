package com.gymsaas.branch.infrastructure.persistence;

import com.gymsaas.branch.domain.BranchSettings;
import com.gymsaas.branch.domain.BranchSettingsRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BranchSettingsRepositoryAdapter implements BranchSettingsRepository {

    private final BranchSettingsJpaRepository repository;

    public BranchSettingsRepositoryAdapter(BranchSettingsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BranchSettings> find(UUID tenantId, UUID branchId) {
        return repository.findByTenantIdAndBranchId(tenantId, branchId).map(this::toDomain);
    }

    @Override
    public BranchSettings save(BranchSettings settings) {
        BranchSettingsJpaEntity entity = repository.findById(settings.id())
                .orElseGet(() -> new BranchSettingsJpaEntity(settings.id(), settings.tenantId(),
                        settings.branchId(), settings.timezone(), settings.openingTime(),
                        settings.closingTime(), settings.maxCapacity(), settings.allow24hAccess(),
                        settings.settings()));
        entity.setTimezone(settings.timezone());
        entity.setOpeningTime(settings.openingTime());
        entity.setClosingTime(settings.closingTime());
        entity.setMaxCapacity(settings.maxCapacity());
        entity.setAllow24hAccess(settings.allow24hAccess());
        entity.setSettings(settings.settings());
        return toDomain(repository.save(entity));
    }

    private BranchSettings toDomain(BranchSettingsJpaEntity entity) {
        return new BranchSettings(entity.getId(), entity.getTenantId(), entity.getBranchId(),
                entity.getTimezone(), entity.getOpeningTime(), entity.getClosingTime(),
                entity.getMaxCapacity(), entity.isAllow24hAccess(),
                entity.getSettings() == null ? Map.of() : entity.getSettings());
    }
}
