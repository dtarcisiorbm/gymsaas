package com.gymsaas.rbac.infrastructure.persistence;

import com.gymsaas.rbac.domain.Permission;
import com.gymsaas.rbac.domain.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository repository;

    public PermissionRepositoryAdapter(PermissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Permission> findAll() {
        return repository.findAll().stream()
                .map(entity -> new Permission(entity.getId(), entity.getCode(), entity.getResource(),
                        entity.getAction(), entity.getDescription()))
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }
}
