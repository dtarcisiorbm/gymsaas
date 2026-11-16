package com.gymsaas.membership.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentMembershipJpaRepository extends JpaRepository<StudentMembershipJpaEntity, UUID> {

    Optional<StudentMembershipJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<StudentMembershipJpaEntity> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    Optional<StudentMembershipJpaEntity> findFirstByTenantIdAndRenewedFromIdAndStatusNot(UUID tenantId,
                                                                                         UUID renewedFromId,
                                                                                         String status);

    List<StudentMembershipJpaEntity> findByTenantIdAndStatusAndStartDateLessThanEqual(UUID tenantId, String status,
                                                                                     LocalDate date);

    List<StudentMembershipJpaEntity> findByTenantIdAndStatusInAndEndDateLessThan(UUID tenantId,
                                                                                Collection<String> statuses,
                                                                                LocalDate date);

    @Query("""
            select count(m) > 0 from StudentMembershipJpaEntity m
            where m.tenantId = :tenantId and m.studentId = :studentId and m.planId = :planId
              and m.status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'FROZEN')
              and m.startDate <= :endDate and m.endDate >= :startDate
              and m.id <> :excludeId
            """)
    boolean existsOpenOverlap(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                              @Param("planId") UUID planId, @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate, @Param("excludeId") UUID excludeId);

    @Query(value = """
            select m from StudentMembershipJpaEntity m
            where m.tenantId = :tenantId
              and (:status = '' or m.status = :status)
              and (:studentId is null or m.studentId = :studentId)
              and (:planId is null or m.planId = :planId)
              and (:branchId is null or m.branchId = :branchId)
              and m.endDate >= :endingFrom and m.endDate <= :endingTo
              and (:search = ''
                   or lower(m.registrationNumber) like lower(concat('%', :search, '%'))
                   or exists (select 1 from StudentJpaEntity s
                              where s.id = m.studentId and s.tenantId = m.tenantId
                                and (lower(s.name) like lower(concat('%', :search, '%'))
                                     or lower(s.registrationNumber) like lower(concat('%', :search, '%')))))
            """,
            countQuery = """
            select count(m) from StudentMembershipJpaEntity m
            where m.tenantId = :tenantId
              and (:status = '' or m.status = :status)
              and (:studentId is null or m.studentId = :studentId)
              and (:planId is null or m.planId = :planId)
              and (:branchId is null or m.branchId = :branchId)
              and m.endDate >= :endingFrom and m.endDate <= :endingTo
              and (:search = ''
                   or lower(m.registrationNumber) like lower(concat('%', :search, '%'))
                   or exists (select 1 from StudentJpaEntity s
                              where s.id = m.studentId and s.tenantId = m.tenantId
                                and (lower(s.name) like lower(concat('%', :search, '%'))
                                     or lower(s.registrationNumber) like lower(concat('%', :search, '%')))))
            """)
    Page<StudentMembershipJpaEntity> search(@Param("tenantId") UUID tenantId, @Param("status") String status,
                                            @Param("studentId") UUID studentId, @Param("planId") UUID planId,
                                            @Param("branchId") UUID branchId,
                                            @Param("endingFrom") LocalDate endingFrom,
                                            @Param("endingTo") LocalDate endingTo,
                                            @Param("search") String search, Pageable pageable);
}
