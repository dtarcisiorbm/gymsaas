package com.gymsaas.tenant.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {

    Optional<TenantJpaEntity> findByCnpj(String cnpj);

    Optional<TenantJpaEntity> findByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    @Query(value = """
            select t from TenantJpaEntity t
            where (:status = '' or t.status = :status)
              and (:search = ''
                   or lower(t.name) like lower(concat('%', :search, '%'))
                   or lower(t.legalName) like lower(concat('%', :search, '%'))
                   or t.document like concat('%', :search, '%'))
            order by t.name
            """,
            countQuery = """
            select count(t) from TenantJpaEntity t
            where (:status = '' or t.status = :status)
              and (:search = ''
                   or lower(t.name) like lower(concat('%', :search, '%'))
                   or lower(t.legalName) like lower(concat('%', :search, '%'))
                   or t.document like concat('%', :search, '%'))
            """)
    Page<TenantJpaEntity> search(@Param("status") String status,
                                 @Param("search") String search,
                                 Pageable pageable);
}
